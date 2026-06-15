const config = window.SITE_CONFIG;
const $ = (selector) => document.querySelector(selector);

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove("show"), 2400);
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
  $("#avatar").textContent = config.profile.initials;
  $("#pixKey").textContent = config.pix.key;
  $("#livePixLink").href = config.pix.livePixUrl;
  $("#year").textContent = new Date().getFullYear();

  $("#socialMini").innerHTML = config.socials.map(s => `<button class="social-pill" data-hub="${s.hub || ""}" data-url="${s.url}" aria-label="${s.label}"><b>${s.icon}</b><span>${s.label}</span></button>`).join("");

  $("#linkGrid").innerHTML = config.links.map(link => `
    <button class="link-card ${link.featured ? "featured" : ""}" data-hub="${link.hub || ""}" data-url="${link.url}">
      <span class="link-icon">${link.icon}</span><span class="link-copy"><strong>${link.title}</strong><small>${link.subtitle}</small></span><span class="link-arrow">↗</span>
    </button>`).join("");

  const videoCards = config.videos.map((video, index) => `
    <button class="video-card" data-video="${index}" style="--accent:${video.accent}">
      <span class="video-visual"><i class="play">▶</i><b>ESTY<br>NINE</b><em>0${index + 1}</em></span>
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
    $("#modalDescription").textContent = video.meta + ". Troque este conteúdo no arquivo site-config.js.";
    $("#modalLink").href = video.url;
    $("#modalMedia").innerHTML = `<span>ESTY<br>NINE</span><button aria-hidden="true">▶</button>`;
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
  const data = { title: "Estynine", text: "Vem conhecer a Estynine!", url: location.href };
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
