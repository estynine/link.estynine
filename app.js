const config = window.SITE_CONFIG;
const $ = (selector) => document.querySelector(selector);

const platformIcons = {
  instagram: `<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="5"/><circle cx="12" cy="12" r="4.2"/><circle cx="17.4" cy="6.7" r="1" class="fill"/></svg>`,
  tiktok: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 3v10.1a4.6 4.6 0 1 1-3.9-4.55v3.1a1.7 1.7 0 1 0 1 1.55V3h2.9Z"/><path d="M15 3c.45 2.5 1.9 4 4.5 4.35v3.05A7.7 7.7 0 0 1 15 8.7"/></svg>`,
  youtube: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M21 8.1a3 3 0 0 0-2.1-2.15C17.05 5.45 12 5.45 12 5.45s-5.05 0-6.9.5A3 3 0 0 0 3 8.1 31 31 0 0 0 2.5 12 31 31 0 0 0 3 15.9a3 3 0 0 0 2.1 2.15c1.85.5 6.9.5 6.9.5s5.05 0 6.9-.5A3 3 0 0 0 21 15.9a31 31 0 0 0 .5-3.9 31 31 0 0 0-.5-3.9Z"/><path d="m10 9 5 3-5 3Z" class="fill-bg"/></svg>`,
  whatsapp: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20.5 11.7a8.45 8.45 0 0 1-12.55 7.4L3.5 20.5l1.45-4.3A8.45 8.45 0 1 1 20.5 11.7Z"/><path d="M8.1 7.4c.2-.45.4-.45.75-.45h.55c.2 0 .4.05.5.4l.7 1.7c.1.3.05.45-.15.7l-.55.65c-.2.2-.2.4-.05.65.75 1.3 1.8 2.35 3.15 3 .25.1.45.1.65-.1l.8-.95c.2-.25.4-.25.7-.15l1.7.8c.3.15.35.3.3.6-.2 1.1-1.35 2-2.4 2.1-1 .1-2.2-.2-4.05-1.25-2.4-1.35-4-3.75-4.1-5.8-.05-.8.25-1.45.5-1.9Z"/></svg>`,
  discord: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18.9 5.35A16 16 0 0 0 15 4.1l-.5 1a13.8 13.8 0 0 0-5 0l-.55-1A16 16 0 0 0 5.1 5.35C2.65 9 2 12.55 2.3 16.05a15.7 15.7 0 0 0 4.8 2.45l1.15-1.6a10 10 0 0 1-1.8-.9l.45-.35a11.5 11.5 0 0 0 10.2 0l.45.35a10 10 0 0 1-1.8.9l1.15 1.6a15.7 15.7 0 0 0 4.8-2.45c.4-4.05-.7-7.55-2.8-10.7ZM8.6 14.25c-1 0-1.8-.9-1.8-2s.8-2 1.8-2 1.8.9 1.8 2-.8 2-1.8 2Zm6.8 0c-1 0-1.8-.9-1.8-2s.8-2 1.8-2 1.8.9 1.8 2-.8 2-1.8 2Z"/></svg>`,
  livepix: `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3.2c4.9 0 8.8 3.2 8.8 7.3 0 2.45-1.4 4.65-3.65 6l.55 3.3-3.65-1.8c-.65.1-1.35.2-2.05.2-4.9 0-8.8-3.45-8.8-7.7S7.1 3.2 12 3.2Z"/><path d="m10.7 7.4 4.8 3.1-4.8 3.1Z" class="fill-bg"/></svg>`,
  live: `<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="3"/><path d="M7.75 7.75a6 6 0 0 0 0 8.5M16.25 7.75a6 6 0 0 1 0 8.5M4.6 4.6a10.5 10.5 0 0 0 0 14.8M19.4 4.6a10.5 10.5 0 0 1 0 14.8"/></svg>`
};

function iconFor(item) {
  const value = `${item.label || ""} ${item.title || ""}`.toLowerCase();
  for (const name of ["instagram", "tiktok", "youtube", "whatsapp", "discord", "livepix"]) {
    if (value.includes(name)) return platformIcons[name];
  }
  if (value.includes("live")) return platformIcons.live;
  return item.icon;
}

function syncPageWidth() {
  document.documentElement.style.setProperty("--page-width", `${document.documentElement.clientWidth}px`);
}
syncPageWidth();
window.addEventListener("resize", syncPageWidth);

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove("show"), 2400);
}

function mediaMarkup(video) {
  const watermark = `<span class="media-watermark"><b>E9</b> ESTYNINE</span>`;
  if (video.media) return `<video src="${video.media}" poster="${video.thumbnail || ""}" muted loop playsinline preload="metadata"></video>${watermark}`;
  if (video.thumbnail) return `<img src="${video.thumbnail}" alt="${video.title}" loading="lazy" onerror="this.remove()">${watermark}`;
  return `${watermark}<b class="media-fallback">ESTY<br>NINE</b>`;
}

function openHub(key) {
  const hub = config.hubs[key];
  if (!hub) return;
  $("#hubKicker").textContent = hub.kicker;
  $("#hubTitle").textContent = hub.title;
  $("#hubDescription").textContent = hub.description;
  $("#hubOfficial").href = hub.official;
  $("#hubGrid").innerHTML = hub.items.map((item, index) => `
    <article class="hub-card" style="--delay:${index * 70}ms">
      <span>${String(index + 1).padStart(2, "0")}</span><div class="hub-art"><i></i></div><h3>${item}</h3><p>Ver conteúdo <b>↗</b></p>
    </article>`).join("");
  $("#hubModal").showModal();
}

function render() {
  $("#profileName").textContent = config.profile.name;
  $("#profileBio").textContent = config.profile.bio;
  $("#avatar").innerHTML = config.profile.avatar
    ? `<img src="${config.profile.avatar}" alt="Foto de ${config.profile.name}" onerror="this.remove();this.parentElement.textContent='${config.profile.initials}'">`
    : config.profile.initials;
  $("#pixKey").textContent = config.pix.key;
  $("#livePixLink").href = config.pix.livePixUrl;
  $("#year").textContent = new Date().getFullYear();

  $("#socialMini").innerHTML = config.socials.map(s => `<button class="social-pill" data-hub="${s.hub || ""}" data-url="${s.url}" aria-label="${s.label}"><b>${iconFor(s)}</b><span>${s.label}</span></button>`).join("");

  $("#linkGrid").innerHTML = config.links.map(link => `
    <button class="link-card ${link.featured ? "featured" : ""}" data-hub="${link.hub || ""}" data-url="${link.url}">
      <span class="link-icon">${iconFor(link)}</span><span class="link-copy"><strong>${link.title}</strong><small>${link.subtitle}</small></span><span class="link-arrow">↗</span>
    </button>`).join("");

  $("#eventsGrid").innerHTML = config.events.map(event => `
    <article class="event-card ${event.featured ? "featured-event" : ""}">
      <span class="event-badge">${event.date}</span>
      <h3>${event.title}</h3>
      <p>${event.description}</p>
      <small>${event.time}</small>
      <a href="${event.url}" target="_blank" rel="noopener">${event.cta} ↗</a>
    </article>`).join("");

  const videoCards = config.videos.map((video, index) => `
    <button class="video-card" data-video="${index}" style="--accent:${video.accent}">
      <span class="video-visual">${mediaMarkup(video)}<i class="play">▶</i><em>0${index + 1}</em></span>
      <span class="video-info"><small>${video.platform}</small><strong>${video.title}</strong><span>${video.meta} <b>↗</b></span></span>
    </button>`).join("");
  $("#videoTrack").innerHTML = videoCards + videoCards;

  document.querySelectorAll("[data-hub]").forEach(button => button.addEventListener("click", () => {
    if (button.dataset.hub) openHub(button.dataset.hub);
    else if (button.dataset.url && button.dataset.url !== "#") window.open(button.dataset.url, "_blank", "noopener");
    else showToast("Link chegando em breve");
  }));

  document.querySelectorAll("[data-video]").forEach(card => card.addEventListener("click", () => {
    const video = config.videos[Number(card.dataset.video)];
    $("#modalPlatform").textContent = video.platform;
    $("#modalTitle").textContent = video.title;
    $("#modalDescription").textContent = video.meta + ". Conteúdo oficial do Estynine.";
    $("#modalLink").href = video.url;
    $("#modalMedia").innerHTML = `${mediaMarkup(video)}<button aria-hidden="true">▶</button>`;
    const modalVideo = $("#modalMedia video");
    if (modalVideo) modalVideo.play().catch(() => {});
    $("#videoModal").showModal();
  }));
}

render();

document.querySelectorAll("[data-close]").forEach(button => button.addEventListener("click", () => button.closest("dialog").close()));
document.querySelectorAll("dialog").forEach(dialog => dialog.addEventListener("click", event => { if (event.target === dialog) dialog.close(); }));

$("#copyPix").addEventListener("click", async () => {
  try { await navigator.clipboard.writeText(config.pix.key); showToast("Pix copiado com sucesso"); }
  catch { showToast(`Chave Pix: ${config.pix.key}`); }
});

$("#shareButton").addEventListener("click", async () => {
  const data = { title: "Estynine", text: "Vem conhecer o Estynine!", url: location.href };
  try { if (navigator.share) await navigator.share(data); else { await navigator.clipboard.writeText(location.href); showToast("Link copiado"); } } catch {}
});

const track = $("#videoTrack");
$("#prevVideo").addEventListener("click", () => track.scrollBy({ left: -300, behavior: "smooth" }));
$("#nextVideo").addEventListener("click", () => track.scrollBy({ left: 300, behavior: "smooth" }));
let carouselPaused = false;
let lastFrame = performance.now();
let carouselPosition = track.scrollLeft;
function moveCarousel(now) {
  const elapsed = Math.min(now - lastFrame, 40);
  lastFrame = now;
  if (!carouselPaused && track.scrollWidth > track.clientWidth) {
    carouselPosition += elapsed * 0.035;
    const halfway = track.scrollWidth / 2;
    if (carouselPosition >= halfway) carouselPosition -= halfway;
    track.scrollLeft = carouselPosition;
  }
  requestAnimationFrame(moveCarousel);
}
track.addEventListener("pointerdown", () => { carouselPaused = true; });
window.addEventListener("pointerup", () => { carouselPosition = track.scrollLeft; carouselPaused = false; lastFrame = performance.now(); });
requestAnimationFrame(moveCarousel);

const logoSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100"><rect width="100" height="100" rx="30" fill="#b90025"/><text x="50" y="59" text-anchor="middle" fill="white" font-family="Arial,sans-serif" font-size="35" font-weight="800">E9</text></svg>`;
let styledQr;
if (window.QRCodeStyling) {
  styledQr = new QRCodeStyling({
    width: 168, height: 168, type: "canvas", data: config.profile.pageUrl,
    image: `data:image/svg+xml;charset=utf-8,${encodeURIComponent(logoSvg)}`,
    margin: 8,
    qrOptions: { errorCorrectionLevel: "H" },
    imageOptions: { hideBackgroundDots: true, imageSize: 0.28, margin: 5, crossOrigin: "anonymous" },
    dotsOptions: { type: "rounded", gradient: { type: "linear", rotation: 0.7, colorStops: [{ offset: 0, color: "#ff2148" }, { offset: 0.5, color: "#a90020" }, { offset: 1, color: "#32000a" }] } },
    backgroundOptions: { color: "#fffafb" },
    cornersSquareOptions: { type: "extra-rounded", color: "#760017" },
    cornersDotOptions: { type: "dot", color: "#ff2148" }
  });
  styledQr.append($("#qrCode"));
} else $("#qrCode").textContent = "QR indisponível offline";

$("#qrDownload").addEventListener("click", () => {
  if (!styledQr) return showToast("QR ainda está carregando");
  styledQr.download({ name: "qrcode-estynine-oficial", extension: "png" });
});

const observer = new IntersectionObserver(entries => entries.forEach(entry => { if (entry.isIntersecting) entry.target.classList.add("visible"); }), { threshold: 0.12 });
document.querySelectorAll(".reveal").forEach(element => observer.observe(element));