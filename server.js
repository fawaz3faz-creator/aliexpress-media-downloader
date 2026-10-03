const express = require('express');
const path = require('path');
const axios = require('axios');
const cheerio = require('cheerio');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.static(path.join(__dirname, 'public')));

function cleanUrl(raw) {
  if (!raw) return '';
  let value = String(raw).trim()
    .replace(/\\u003d/gi, '=')
    .replace(/\\\//g, '/')
    .replace(/&amp;/gi, '&')
    .replace(/\"/g, '')
    .replace(/\'/g, '')
    .replace(/[),\]]+$/g, '')
    .trim();

  if (!value) return '';
  if (/^https?:\/\//i.test(value)) {
    return value;
  }
  return '';
}

function boostToHighestQuality(rawUrl) {
  let out = (rawUrl || '').trim();
  out = out.replace(/_\d+x\d+/gi, '');
  out = out.replace(/_(?:640x640|400x400|300x300|220x220|120x120)/gi, '');
  out = out.replace(/[?&]p=\d+/gi, '');
  out = out.replace(/[?&]w=\d+/gi, '');
  return out;
}

function classifyMedia(url) {
  const lower = (url || '').toLowerCase();
  if (/(\.mp4|\.m3u8|\.webm|\.mpd|video)/i.test(lower)) return 'video';
  return 'image';
}

function resolveQuality(url) {
  const lower = (url || '').toLowerCase();
  if (/(\.mp4|\.m3u8|\.webm|\.mpd)/i.test(lower)) return 'HD / Original';
  if (/(?:_1000|_1200|_1500|_2000|_2500)/i.test(lower)) return 'HD / Original';
  if (/(?:_640|_400|_300|_220|_120)/i.test(lower)) return 'HD';
  return 'Original';
}

async function resolveRedirectUrl(rawUrl) {
  const target = rawUrl.trim();
  const response = await axios.get(target, {
    maxRedirects: 12,
    validateStatus: () => true,
    timeout: 20000,
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36',
      'Accept-Language': 'en-US,en;q=0.9',
      'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
    }
  });

  return response.request && response.request.res && response.request.res.responseUrl
    ? response.request.res.responseUrl
    : response.config.url || target;
}

function collectMediaUrls(html) {
  const urls = new Set();
  const regex = /(https?:\/\/[^\s"'<>]+(?:\.(?:jpe?g|png|webp|gif|bmp|avif|jpeg|mp4|m3u8|webm|mpd))(?:\?[^\s"'<>]+)?)/gi;

  let match;
  while ((match = regex.exec(html)) !== null) {
    const cleaned = cleanUrl(match[0]);
    if (cleaned) urls.add(cleaned);
  }

  const $ = cheerio.load(html);

  $('img, source, video, a').each((_, element) => {
    const attrs = [
      $(element).attr('src'),
      $(element).attr('data-src'),
      $(element).attr('data-lazy'),
      $(element).attr('srcset'),
      $(element).attr('poster'),
      $(element).attr('href')
    ];

    attrs.forEach((attr) => {
      if (!attr) return;
      if (attr.includes(',')) {
        attr.split(',').forEach((part) => {
          const candidate = cleanUrl(part.trim().split(' ')[0]);
          if (candidate) urls.add(candidate);
        });
        return;
      }

      const clean = cleanUrl(attr);
      if (clean) urls.add(clean);
    });
  });

  const scriptBlocks = $('script').toArray();
  scriptBlocks.forEach((script) => {
    const text = $(script).html() || '';
    if (!text) return;
    const scriptMatches = text.matchAll(/https?:\/\/[^\s"'<>]+/gi);
    for (const scriptMatch of scriptMatches) {
      const cleaned = cleanUrl(scriptMatch[0]);
      if (cleaned) urls.add(cleaned);
    }
  });

  return Array.from(urls)
    .filter((url) => /\.(jpg|jpeg|png|webp|gif|bmp|avif|mp4|m3u8|webm|mpd)(\?.*)?$/i.test(url) || /video/i.test(url))
    .map((url) => {
      const bestUrl = boostToHighestQuality(url);
      return {
        id: `${bestUrl}-${Math.random().toString(16).slice(2)}`,
        url: bestUrl,
        type: classifyMedia(bestUrl),
        quality: resolveQuality(bestUrl),
        title: `Media ${bestUrl.includes('.mp4') || bestUrl.includes('.webm') ? 'Video' : 'Image'}`
      };
    })
    .filter((item, idx, arr) => arr.findIndex((x) => x.url === item.url) === idx)
    .slice(0, 200);
}

app.get('/api/health', (_, res) => {
  res.json({ ok: true, message: 'AliExpress downloader web API is running.' });
});

app.post('/api/fetch-media', async (req, res) => {
  try {
    const { url } = req.body || {};

    if (!url || !String(url).trim()) {
      return res.status(400).json({ error: 'Please provide a valid AliExpress URL.' });
    }

    const target = String(url).trim();
    const finalUrl = await resolveRedirectUrl(target);
    const htmlResponse = await axios.get(finalUrl, {
      timeout: 25000,
      maxRedirects: 12,
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36',
        'Accept-Language': 'en-US,en;q=0.9',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
      },
      validateStatus: () => true
    });

    const html = String(htmlResponse.data || '');
    const items = collectMediaUrls(html);

    if (!items.length) {
      return res.json({
        finalUrl,
        items: [],
        message: 'No media was found on this page. Try a product page with visible gallery or video assets.'
      });
    }

    return res.json({ finalUrl, items, message: 'Media extracted successfully.' });
  } catch (error) {
    console.error('Error fetching media:', error.message);
    return res.status(500).json({
      error: 'Could not load the page or extract media. Try a product page or a direct AliExpress product URL.'
    });
  }
});

app.get('*', (_, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
  console.log(`AliExpress Media Downloader web app running on http://localhost:${PORT}`);
});
