export type VideoProvider = "youtube" | "tiktok" | "instagram" | "mp4" | "external";

export type VideoItem = {
  id: string;
  title: string;
  provider: VideoProvider;
  url: string;
  embedUrl?: string;
  thumbnail?: string;
  description?: string;
  views?: string;
  featured?: boolean;
};

function youtubeId(url: string) {
  try {
    const parsed = new URL(url);
    if (parsed.hostname.includes("youtu.be")) return parsed.pathname.split("/").filter(Boolean)[0] || "";
    if (parsed.pathname.startsWith("/shorts/")) return parsed.pathname.split("/").filter(Boolean)[1] || "";
    if (parsed.pathname.startsWith("/embed/")) return parsed.pathname.split("/").filter(Boolean)[1] || "";
    return parsed.searchParams.get("v") || "";
  } catch {
    return "";
  }
}

function fallback(video: VideoItem, label = "Abrir vídeo") {
  return (
    <div className="embed-fallback">
      {video.thumbnail ? <img src={video.thumbnail} alt={video.title} /> : <span className="fallback-mark">ESTYNINE</span>}
      <div>
        <h3>{video.title}</h3>
        <p>{video.description || video.views || "Este conteúdo abre fora do site."}</p>
        <a href={video.url} target="_blank" rel="noopener noreferrer">{label} ↗</a>
      </div>
    </div>
  );
}

export function SmartVideoEmbed({ video }: { video: VideoItem }) {
  if (video.provider === "youtube") {
    const id = youtubeId(video.url);
    if (!id) return fallback(video, "Abrir no YouTube");
    return <div className="smart-embed ratio-16x9"><iframe src={`https://www.youtube.com/embed/${id}`} title={video.title} allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen /></div>;
  }

  if (video.provider === "mp4") {
    return <div className="smart-embed"><video controls playsInline preload="metadata" poster={video.thumbnail}><source src={video.url} type="video/mp4" /></video></div>;
  }

  if (video.provider === "tiktok") {
    const id = video.url.match(/\/video\/(\d+)/)?.[1];
    const embed = video.embedUrl || (id ? `https://www.tiktok.com/embed/v2/${id}` : "");
    if (!embed) return fallback(video, "Assistir no TikTok");
    return <><div className="smart-embed vertical-embed"><iframe src={embed} title={video.title} allow="encrypted-media; fullscreen" /></div><div className="embed-help"><a href={video.url} target="_blank" rel="noopener noreferrer">Assistir no TikTok ↗</a></div></>;
  }

  if (video.provider === "instagram") {
    const clean = video.url.split("?")[0].replace(/\/$/, "");
    const embed = video.embedUrl || (/instagram\.com\/(p|reel|tv)\//.test(clean) ? `${clean}/embed` : "");
    if (!embed) return fallback(video, "Abrir no Instagram");
    return <><div className="smart-embed vertical-embed"><iframe src={embed} title={video.title} allowFullScreen /></div><div className="embed-help"><a href={video.url} target="_blank" rel="noopener noreferrer">Abrir no Instagram ↗</a></div></>;
  }

  return fallback(video, "Abrir conteúdo");
}