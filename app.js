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
      <span>${String(index + 1).padStart(2, "0")}</span><div class="hub-art"><i></i></div><h3>${item}</h3><p>Ver conteÃºdo <b>â†—</b></p>
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
      <span class="link-icon">${link.icon}</span><span class="link-copy"><strong>${link.title}</strong><small>${link.subtitle}</small></span><span class="link-arrow">â†—</span>
    </button>`).join("");

  $("#videoTrack").innerHTML = config.videos.map((video, index) => `
    <button class="video-card" data-video="${index}" style="--accent:${video.accent}">
      <span class="video-visual"><i class="play">â–¶</i><b>ESTY<br>NINE</b><em>0${index + 1}</em></span>
      <span class="video-info"><small>${video.platform}</small><strong>${video.title}</strong><span>${video.meta} <b>â†—</b></span></span>
    </button>`).join("");

  document.querySelectorAll("[data-hub]").forEach(button => button.addEventListener("click", () => {
    if (button.dataset.hub) openHub(button.dataset.hub);
    else if (button.dataset.url && button.dataset.url !== "#") window.open(button.dataset.url, "_blank", "noopener");
    else showToast("Link chegando em breve");
  }));

  document.querySelectorAll("[data-video]").forEach(card => card.addEventListener("click", () => {
    const video = config.videos[Number(card.dataset.video)];
    $("#modalPlatform").textContent = video.platform;
    $("#modalTitle").textContent = video.title;
    $("#modalDescription").textContent = video.meta + ". Troque este conteÃºdo no arquivo site-config.js.";
    $("#modalLink").href = video.url;
    $("#modalMedia").innerHTML = `<span>ESTY<br>NINE</span><button aria-hidden="true">â–¶</button>`;
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
let autoScroll = setInterval(() => { const atEnd = track.scrollLeft + track.clientWidth >= track.scrollWidth - 10; track.scrollTo({ left: atEnd ? 0 : track.scrollLeft + 292, behavior: "smooth" }); }, 4200);
track.addEventListener("pointerdown", () => clearInterval(autoScroll), { once: true });

if (window.QRCode) new QRCode($("#qrCode"), { text: config.profile.pageUrl, width: 138, height: 138, colorDark: "#140608", colorLight: "#ffffff", correctLevel: QRCode.CorrectLevel.H });
else $("#qrCode").textContent = "QR indisponÃ­vel offline";

$("#qrDownload").addEventListener("click", () => {
  const image = $("#qrCode img") || $("#qrCode canvas");
  if (!image) return showToast("QR ainda estÃ¡ carregando");
  const link = document.createElement("a");
  link.download = "qrcode-estynine.png";
  link.href = image.src || image.toDataURL("image/png");
  link.click();
});

const observer = new IntersectionObserver(entries => entries.forEach(entry => { if (entry.isIntersecting) entry.target.classList.add("visible"); }), { threshold: 0.12 });
document.querySelectorAll(".reveal").forEach(element => observer.observe(element));
