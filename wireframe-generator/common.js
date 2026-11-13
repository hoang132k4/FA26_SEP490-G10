const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

function uid() {
  return crypto.randomBytes(16).toString('hex');
}

function xmlEscape(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function rectShape({ id = uid(), x = 0, y = 0, w, h, rx = 0, ry = 0, fill = '#FFFFFFFF', stroke = '#E4E9F1FF', strokeW = 1, textContent = '' }) {
  const hexFill = fill.length === 9 ? fill.slice(0, 7) : fill;
  const hexStroke = stroke.length === 9 ? stroke.slice(0, 7) : stroke;
  const fillAlpha = fill.length === 7 ? fill + 'FF' : fill;
  const strokeAlpha = stroke.length === 7 ? stroke + 'FF' : stroke;
  const rectW = Math.max(1, w - strokeW * 2);
  const rectH = Math.max(1, h - strokeW * 2);

  return `<g xmlns="http://www.w3.org/2000/svg" xmlns:p="http://www.evolus.vn/Namespace/Pencil" p:type="Shape" p:def="Evolus.Common:RoundedRect" id="${id}" transform="matrix(1,0,0,1,${x},${y})"><p:metadata><p:property name="box">${w},${h}</p:property><p:property name="withBlur">false</p:property><p:property name="radius">${rx},${ry}</p:property><p:property name="textPadding">0,0</p:property><p:property name="fillColor">${fillAlpha}</p:property><p:property name="strokeColor">${strokeAlpha}</p:property><p:property name="strokeStyle">${strokeW}|</p:property><p:property name="textContent">${xmlEscape(textContent)}</p:property><p:property name="textFont">Sans|normal|normal|13px|none|0</p:property><p:property name="textColor">#1E2A4AFF</p:property><p:property name="textAlign">0,0</p:property></p:metadata><rect x="${strokeW}" y="${strokeW}" width="${rectW}" height="${rectH}" rx="${rx}" ry="${ry}" style="fill:${hexFill};stroke:${hexStroke};stroke-width:${strokeW}" /></g>`;
}

function textShape({ id = uid(), x = 0, y = 0, w = 500, h = 24, text, size = 13, weight = 'normal', color = '#1E2A4A', fixedWidth = true, align = '0,0' }) {
  const hexColor = color.length === 9 ? color.slice(0, 7) : color;
  const colorAlpha = color.length === 7 ? color + 'FF' : color;
  const escaped = xmlEscape(text);
  const fontMeta = `Sans|${weight}|normal|${size}px|none|0`;

  return `<g xmlns="http://www.w3.org/2000/svg" xmlns:p="http://www.evolus.vn/Namespace/Pencil" p:type="Shape" p:def="Evolus.Common:PlainTextV3" id="${id}" transform="matrix(1,0,0,1,${x},${y})"><p:metadata><p:property name="disabled">false</p:property><p:property name="width">${w},0</p:property><p:property name="fixedWidth">${fixedWidth}</p:property><p:property name="label">${escaped}</p:property><p:property name="textColor">${colorAlpha}</p:property><p:property name="textFont">${fontMeta}</p:property><p:property name="textAlign">${align}</p:property></p:metadata><rect x="0" y="0" width="${w}" height="${h}" style="fill:none;stroke:none" /><text xml:space="preserve" dominant-baseline="hanging" style="fill:${hexColor};font-family:Arial,sans-serif;font-size:${size}px;font-weight:${weight}"><tspan x="0" y="0">${escaped}</tspan></text></g>`;
}

function badge({ x, y, text, bg = '#173B57', color = '#FFFFFF', w = null, h = 24, rx = 12, fontSize = 11 }) {
  const width = w || (text.length * 8 + 18);
  const shapes = [];
  shapes.push(rectShape({ x, y, w: width, h, rx, ry: rx, fill: bg, stroke: bg }));
  shapes.push(textShape({ x: x + 8, y: y + Math.floor((h - fontSize) / 2) - 1, w: width - 16, h: fontSize + 6, text, size: fontSize, weight: 'bold', color, align: '0.5,0.5' }));
  return shapes.join('');
}

function button({ x, y, w = 120, h = 36, text, variant = 'primary', rx = 18, fontSize = 13 }) {
  let fill, stroke, textColor;
  if (variant === 'sakura' || variant === 'danger') {
    fill = '#E06D53'; stroke = '#E06D53'; textColor = '#FFFFFF';
  } else if (variant === 'steel' || variant === 'primary') {
    fill = '#173B57'; stroke = '#173B57'; textColor = '#FFFFFF';
  } else if (variant === 'success') {
    fill = '#10B981'; stroke = '#10B981'; textColor = '#FFFFFF';
  } else if (variant === 'outline-steel') {
    fill = '#FFFFFF'; stroke = '#173B57'; textColor = '#173B57';
  } else if (variant === 'outline-danger' || variant === 'outline-sakura') {
    fill = '#FFFFFF'; stroke = '#E06D53'; textColor = '#E06D53';
  } else if (variant === 'outline-primary') {
    fill = '#FFFFFF'; stroke = '#2563EB'; textColor = '#2563EB';
  } else { // secondary / light
    fill = '#FFFFFF'; stroke = '#CBD5E1'; textColor = '#334155';
  }
  const shapes = [];
  shapes.push(rectShape({ x, y, w, h, rx, ry: rx, fill, stroke }));
  shapes.push(textShape({ x: x + 8, y: y + Math.floor((h - fontSize) / 2) - 1, w: w - 16, h: fontSize + 6, text, size: fontSize, weight: 'bold', color: textColor, align: '0.5,0.5' }));
  return shapes.join('');
}

function progressBar({ x, y, w, percent = 50, h = 8, fill = '#10B981' }) {
  const shapes = [];
  shapes.push(rectShape({ x, y, w, h, rx: 4, ry: 4, fill: '#E2E8F0', stroke: '#E2E8F0' }));
  const fillW = Math.max(4, Math.floor((w * percent) / 100));
  shapes.push(rectShape({ x, y, w: fillW, h, rx: 4, ry: 4, fill, stroke: fill }));
  return shapes.join('');
}

// 100% REAL NAVBAR FROM templates/fragments/header.html
function realNavbar({ activeMenu = 'my-courses' }) {
  const shapes = [];
  // 1. Full page background
  shapes.push(rectShape({ x: 0, y: 0, w: 1440, h: 900, rx: 0, ry: 0, fill: '#F8FAFC', stroke: '#F8FAFC' }));

  // 2. Navbar container (68px high)
  shapes.push(rectShape({ x: 0, y: 0, w: 1440, h: 68, rx: 0, ry: 0, fill: '#FFFFFF', stroke: '#E2E8F0' }));

  // Logo Brand: Kotoba logo circle (Salmon #E06D53 + Navy) + Text
  shapes.push(rectShape({ x: 40, y: 15, w: 38, h: 38, rx: 19, ry: 19, fill: '#E06D53', stroke: '#E06D53' }));
  shapes.push(textShape({ x: 40, y: 22, w: 38, h: 26, text: '文', size: 18, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

  shapes.push(textShape({ x: 86, y: 16, w: 120, h: 24, text: 'JEPS', size: 20, weight: 'bold', color: '#173B57' }));
  shapes.push(textShape({ x: 86, y: 39, w: 120, h: 16, text: '日本語試験対策', size: 10, color: '#64748B' }));

  // Nav Items: Home, Courses, Mock Exam, Flashcard, Assessment, Learning Path
  const navItems = [
    { key: 'home', label: 'Trang chủ', x: 230 },
    { key: 'courses', label: 'Khóa học JLPT ▾', x: 330 },
    { key: 'my-courses', label: 'Khóa học của tôi', x: 475 },
    { key: 'mock-exam', label: 'Thi thử JLPT', x: 625 },
    { key: 'flashcards', label: 'Từ vựng Flashcard', x: 745 },
    { key: 'learning-path', label: 'Lộ trình học', x: 910 }
  ];

  navItems.forEach(item => {
    const isActive = (item.key === activeMenu);
    if (isActive) {
      shapes.push(textShape({ x: item.x, y: 24, w: 140, h: 22, text: item.label, size: 14, weight: 'bold', color: '#173B57' }));
      shapes.push(rectShape({ x: item.x - 4, y: 64, w: 120, h: 3, rx: 2, ry: 2, fill: '#E06D53', stroke: '#E06D53' }));
    } else {
      shapes.push(textShape({ x: item.x, y: 24, w: 140, h: 22, text: item.label, size: 14, weight: 'normal', color: '#334155' }));
    }
  });

  // Right Side: Notification Bell + Profile Pill
  // Notification Circle
  shapes.push(rectShape({ x: 1140, y: 16, w: 38, h: 38, rx: 19, ry: 19, fill: '#F1F5F9', stroke: '#E2E8F0' }));
  shapes.push(textShape({ x: 1140, y: 24, w: 38, h: 20, text: '🔔', size: 14, color: '#173B57', align: '0.5,0.5' }));
  shapes.push(rectShape({ x: 1164, y: 12, w: 18, h: 18, rx: 9, ry: 9, fill: '#EF4444', stroke: '#FFFFFF' }));
  shapes.push(textShape({ x: 1164, y: 14, w: 18, h: 14, text: '3', size: 9, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));

  // User Profile Pill (as coded in line 282 of header.html)
  shapes.push(rectShape({ x: 1195, y: 15, w: 205, h: 38, rx: 19, ry: 19, fill: '#E2E8F0', stroke: '#CBD5E1' }));
  // Avatar Circle
  shapes.push(rectShape({ x: 1199, y: 19, w: 30, h: 30, rx: 15, ry: 15, fill: '#173B57', stroke: '#173B57' }));
  shapes.push(textShape({ x: 1199, y: 24, w: 30, h: 20, text: 'N', size: 13, weight: 'bold', color: '#FFFFFF', align: '0.5,0.5' }));
  // User name
  shapes.push(textShape({ x: 1235, y: 24, w: 115, h: 20, text: 'Nguyễn Văn Học...', size: 12, weight: 'bold', color: '#173B57' }));
  // N4 Badge
  shapes.push(rectShape({ x: 1358, y: 23, w: 32, h: 22, rx: 6, ry: 6, fill: '#FFFFFF', stroke: '#CBD5E1' }));
  shapes.push(textShape({ x: 1358, y: 26, w: 32, h: 16, text: 'N4', size: 11, weight: 'bold', color: '#334155', align: '0.5,0.5' }));

  return shapes.join('');
}

function realBreadcrumb(crumbs) {
  const shapes = [];
  let curX = 40;
  crumbs.forEach((c, idx) => {
    const isLast = (idx === crumbs.length - 1);
    const color = isLast ? '#1E293B' : '#64748B';
    const weight = isLast ? 'bold' : 'normal';
    shapes.push(textShape({ x: curX, y: 84, w: c.length * 9 + 10, h: 20, text: c, size: 12, weight, color }));
    curX += c.length * 8 + 14;
    if (!isLast) {
      shapes.push(textShape({ x: curX, y: 84, w: 16, h: 20, text: '/', size: 12, color: '#CBD5E1' }));
      curX += 16;
    }
  });
  return shapes.join('');
}

function createTar(files) {
  const buffers = [];
  for (const file of files) {
    const data = Buffer.isBuffer(file.content) ? file.content : Buffer.from(file.content, 'utf8');
    const header = Buffer.alloc(512);

    header.write(file.name, 0, 100, 'ascii');
    header.write('0000644\0', 100, 8, 'ascii');
    header.write('0000000\0', 108, 8, 'ascii');
    header.write('0000000\0', 116, 8, 'ascii');

    const sizeOctal = data.length.toString(8).padStart(11, '0') + '\0';
    header.write(sizeOctal, 124, 12, 'ascii');

    const mtimeOctal = Math.floor(Date.now() / 1000).toString(8).padStart(11, '0') + '\0';
    header.write(mtimeOctal, 136, 12, 'ascii');

    for (let i = 148; i < 156; i++) header[i] = 0x20;
    header.write('0', 156, 1, 'ascii');
    header.write('ustar\0', 257, 6, 'ascii');
    header.write('00', 263, 2, 'ascii');

    let sum = 0;
    for (let i = 0; i < 512; i++) sum += header[i];
    const chkStr = sum.toString(8).padStart(6, '0') + '\0 ';
    header.write(chkStr, 148, 8, 'ascii');

    buffers.push(header);
    buffers.push(data);
    const pad = (512 - (data.length % 512)) % 512;
    if (pad > 0) buffers.push(Buffer.alloc(pad));
  }
  buffers.push(Buffer.alloc(1024));
  return Buffer.concat(buffers);
}

module.exports = {
  uid,
  xmlEscape,
  rectShape,
  textShape,
  badge,
  button,
  progressBar,
  realNavbar,
  realBreadcrumb,
  createTar
};
