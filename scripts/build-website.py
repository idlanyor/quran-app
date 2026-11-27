"""Build the GitHub Pages website with Python standard library only."""
from pathlib import Path
import html
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'website'
OUT = ROOT / 'build/site'
ORIGIN = 'https://quran.irengcloud.com'
OUT.mkdir(parents=True, exist_ok=True)
shutil.copytree(SOURCE / 'assets', OUT / 'assets', dirs_exist_ok=True)
shutil.copy2(SOURCE / 'site.css', OUT / 'site.css')

def inline(value):
    value = html.escape(value)
    value = re.sub(r'\[([^\]]+)\]\(((?:https://|mailto:)[^)]+)\)', r'<a href="\2">\1</a>', value)
    return re.sub(r'`([^`]+)`', r'<code>\1</code>', value)

def markdown(source):
    blocks = []
    for block in source.strip().split('\n\n'):
        if block.startswith('- '):
            blocks.append('<ul>' + ''.join('<li>' + inline(row[2:]) + '</li>' for row in block.splitlines()) + '</ul>')
        elif block.startswith('## '):
            blocks.append('<h2>' + inline(block[3:]) + '</h2>')
        elif block.startswith('# '):
            blocks.append('<h1>' + inline(block[2:]) + '</h1>')
        else:
            blocks.append('<p>' + inline(block.replace('\n', ' ')) + '</p>')
    return '\n'.join(blocks)

def render(path, title, description, content):
    prefix = '../' if path else './'
    canonical = ORIGIN + '/' + (path + '/' if path else '')
    document = f'''<!doctype html>
<html lang="id"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)}</title><meta name="description" content="{html.escape(description, quote=True)}">
<meta name="theme-color" content="#242338"><link rel="canonical" href="{canonical}">
<meta property="og:type" content="website"><meta property="og:locale" content="id_ID"><meta property="og:site_name" content="Digital Qur’an">
<meta property="og:title" content="{html.escape(title, quote=True)}"><meta property="og:description" content="{html.escape(description, quote=True)}">
<meta property="og:url" content="{canonical}"><meta property="og:image" content="{ORIGIN}/assets/social.png">
<meta name="twitter:card" content="summary_large_image"><meta name="twitter:title" content="{html.escape(title, quote=True)}"><meta name="twitter:image" content="{ORIGIN}/assets/social.png">
<link rel="icon" type="image/png" href="{prefix}assets/icon.png"><link rel="stylesheet" href="{prefix}site.css">
<link rel="preload" href="{prefix}assets/jakarta.ttf" as="font" type="font/ttf" crossorigin>
</head><body><a class="skip" href="#main">Lewati ke konten</a>
<header class="wrap nav"><a class="brand" href="{prefix}" aria-label="Digital Qur’an, beranda"><img src="{prefix}assets/icon.png" width="43" height="43" alt="">Digital Qur’an</a><nav class="nav-links" aria-label="Navigasi utama"><a href="{prefix}#fitur">Fitur</a><a href="{prefix}#faq">Tanya jawab</a><a href="{prefix}privasi/">Privasi</a><a class="nav-cta" href="{prefix}dukungan/">Dukungan</a></nav></header>
<main id="main">{content}</main>
<footer class="wrap footer"><div class="footer-top"><a class="brand" href="{prefix}"><img src="{prefix}assets/icon.png" width="43" height="43" alt="">Digital Qur’an</a><nav class="footer-links" aria-label="Tautan informasi"><a href="{prefix}privasi/">Kebijakan privasi</a><a href="{prefix}ketentuan/">Persyaratan layanan</a><a href="{prefix}hapus-data/">Kelola data</a><a href="{prefix}dukungan/">Dukungan</a><a href="https://github.com/idlanyor/quran-app">GitHub</a></nav></div><div class="footer-bottom"><span>© 2026 Digital Qur’an · Irengcloud</span><span>Pedoman Hidup dalam Genggaman.</span></div></footer>
</body></html>'''
    destination = OUT / path / 'index.html'
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(document)

render('', 'Digital Qur’an — Pedoman Hidup dalam Genggaman', 'Baca Al-Qur’an, pahami tafsir, dengarkan murottal, dan temukan doa serta jadwal Sholat dalam aplikasi Android Digital Qur’an.', (SOURCE / 'index.html').read_text())
render('privasi', 'Kebijakan Privasi — Digital Qur’an', 'Cara Digital Qur’an mengelola data perangkat, lokasi opsional, permintaan konten, dan kartu ayat.', '<article class="legal">' + markdown((ROOT / 'docs/playstore/privacy-policy.md').read_text()) + '</article>')
for slug, title, description in [
    ('ketentuan', 'Persyaratan Layanan — Digital Qur’an', 'Persyaratan penggunaan aplikasi Android Digital Qur’an, sumber konten, layanan, serta kontak pengelola.'),
    ('dukungan', 'Dukungan — Digital Qur’an', 'Bantuan penggunaan, pelaporan masalah bacaan, dan kontak pengelola Digital Qur’an.'),
    ('hapus-data', 'Pengelolaan Data — Digital Qur’an', 'Cara mengelola izin, penanda, audio, cadangan, dan data lokal Digital Qur’an.'),
]:
    render(slug, title, description, (SOURCE / slug / 'index.html').read_text())
render('404-page', 'Halaman tidak ditemukan — Digital Qur’an', 'Kembali ke situs Digital Qur’an.', '<div class="not-found"><p class="eyebrow">404</p><h1>Halaman belum ditemukan.</h1><p>Yuk, kembali ke beranda Digital Qur’an.</p><a class="button" href="/">Buka beranda</a></div>')
# Custom-domain 404 uses root-relative resources because missing routes may have arbitrary depth.
(OUT / '404.html').write_text((OUT / '404-page/index.html').read_text().replace('../', '/'))
shutil.rmtree(OUT / '404-page')
(OUT / '.nojekyll').touch()
(OUT / 'CNAME').write_text('quran.irengcloud.com\n')
(OUT / 'robots.txt').write_text(f'User-agent: *\nAllow: /\nSitemap: {ORIGIN}/sitemap.xml\n')
urls = ['', 'privasi/', 'ketentuan/', 'dukungan/', 'hapus-data/']
(OUT / 'sitemap.xml').write_text('<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">' + ''.join(f'<url><loc>{ORIGIN}/{path}</loc></url>' for path in urls) + '</urlset>\n')
print(f'Built static site at {OUT}')
