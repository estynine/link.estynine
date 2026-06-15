# Estynine Links

Central de links personalizada da streamer **Estynine**, pronta para publicar na Vercel. O projeto Ã© estÃ¡tico, rÃ¡pido e nÃ£o exige instalaÃ§Ã£o de dependÃªncias ou processo de build.

## Rodar localmente

Abra `index.html` no navegador ou use qualquer servidor estÃ¡tico, por exemplo a extensÃ£o Live Server do VS Code.

## Personalizar

Quase todo o conteÃºdo fica em `site-config.js`:

- `profile`: nome, bio, iniciais e endereÃ§o pÃºblico do site;
- `pix`: chave Pix e endereÃ§o do LivePix;
- `socials`: atalhos sociais do topo;
- `links`: botÃµes principais;
- `videos`: cinco destaques do carrossel;
- `hubs`: conteÃºdo das Ã¡reas internas de Instagram, TikTok e YouTube.

Edite `styles.css` para trocar cores, espaÃ§amentos e efeitos. As cores principais ficam nas variÃ¡veis do comeÃ§o do arquivo.

> Antes de publicar, substitua todos os links com `#`, a chave Pix de exemplo e `profile.pageUrl` pelos dados reais.

## Publicar na Vercel

1. Entre em [vercel.com](https://vercel.com) usando sua conta do GitHub.
2. Clique em **Add New > Project** e importe `estynine/link.estynine`.
3. Em **Framework Preset**, escolha **Other**.
4. NÃ£o defina comando de build. Publique o projeto.
5. Copie o domÃ­nio criado pela Vercel para `profile.pageUrl` em `site-config.js`, para o QR Code apontar para o endereÃ§o correto.

TambÃ©m funciona na Netlify arrastando a pasta do projeto para o painel ou conectando o repositÃ³rio.

## Recursos

- layout responsivo e acessÃ­vel;
- carrossel automÃ¡tico com controles e rolagem manual;
- modal de vÃ­deo preparado para links reais;
- hubs internos de conteÃºdo;
- Pix com cÃ³pia e feedback visual;
- QR Code vÃ¡lido com download em PNG;
- compartilhamento nativo ou cÃ³pia do link;
- animaÃ§Ãµes com suporte a `prefers-reduced-motion`.
