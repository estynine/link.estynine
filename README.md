# Estynine Links

Central de links personalizada da streamer **Estynine**, pronta para publicar na Vercel. O projeto é estático, rápido e não exige instalação de dependências ou processo de build.

## Rodar localmente

Abra `index.html` no navegador ou use qualquer servidor estático, por exemplo a extensão Live Server do VS Code.

## Personalizar

Quase todo o conteúdo fica em `site-config.js`:

- `profile`: nome, bio, iniciais e endereço público do site;
- `pix`: chave Pix e endereço do LivePix;
- `socials`: atalhos sociais do topo;
- `links`: botões principais;
- `videos`: cinco destaques do carrossel;
- `hubs`: conteúdo das áreas internas de Instagram, TikTok e YouTube.

Edite `styles.css` para trocar cores, espaçamentos e efeitos. As cores principais ficam nas variáveis do começo do arquivo.

> Antes de publicar, substitua todos os links com `#`, a chave Pix de exemplo e `profile.pageUrl` pelos dados reais.

## Publicar na Vercel

1. Entre em [vercel.com](https://vercel.com) usando sua conta do GitHub.
2. Clique em **Add New > Project** e importe `estynine/link.estynine`.
3. Em **Framework Preset**, escolha **Other**.
4. Não defina comando de build. Publique o projeto.
5. Copie o domínio criado pela Vercel para `profile.pageUrl` em `site-config.js`, para o QR Code apontar para o endereço correto.

Também funciona na Netlify arrastando a pasta do projeto para o painel ou conectando o repositório.

## Recursos

- layout responsivo e acessível;
- carrossel contínuo para a esquerda, com controles e rolagem manual;
- modal de vídeo preparado para links reais;
- hubs internos de conteúdo;
- Pix com cópia e feedback visual;
- QR Code personalizado com gradiente, módulos arredondados, selo E9, alta correção de erro e download em PNG;
- compartilhamento nativo ou cópia do link;
- animações com suporte a `prefers-reduced-motion`.
