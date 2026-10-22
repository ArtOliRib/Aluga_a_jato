# Onde hospedar de graça um projeto Spring Boot 4 + Java 25 + Postgres + SPA (outubro/2026)

Relatório de pesquisa sobre opções de hospedagem **sem custo** para um projeto solo de
portfólio.

**Stack considerada (o projeto real deste repositório):**

- Backend: Spring Boot **4.1.1** (`build.gradle`), Java **25**, `spring-boot-starter-data-jpa`
  + `spring-boot-starter-webmvc` + `validation`, empacotado em **container Docker**
  (`Dockerfile`, `docker-compose.yml`).
- Banco: **PostgreSQL 16** (`postgres:16-alpine` no `docker-compose.yml`).
- Módulo de proteção de dados pessoais (LGPD) que exige **duas chaves secretas** no boot
  (`LOCADORA_DADO_PESSOAL_INDICE_CHAVE`, `LOCADORA_DADO_PESSOAL_CIFRA_CHAVE`); o container
  **falha intencionalmente** sem elas.
- Frontend: SPA (React/Vue + Vite), build estático.

**Data da pesquisa:** 21 de outubro de 2026. Todo número abaixo tem URL e, quando a página
indica, data. Onde não foi possível verificar, está dito explicitamente.

> **Aviso de método.** A ferramenta de busca deste ambiente não retornou resultados, então a
> coleta foi feita por **leitura direta das páginas de documentação e preços** dos fornecedores.
> Preços e limites de tiers gratuitos mudam com frequência; revalide antes de decidir.

---

## Resumo executivo (leia isto primeiro)

1. **Não existe mais um "Heroku dos pobres" garantido e vitalício** para container + Postgres
   em 2026. Quase todo tier gratuito tem uma destas três armadilhas: **dorme**, **expira** ou
   **exige cartão**.
2. A combinação mais confiável hoje, para o seu caso, é:
   - **Backend em container:** **Google Cloud Run** (always-free, *scale to zero*, sem cartão
     cobrado dentro do free tier) **ou** uma VM **Oracle Cloud Always Free** (2 OCPU / 12 GB ARM,
     se você aceitar administrar servidor).
   - **Postgres gerenciado:** **Neon** (free permanente, sem cartão, *scale-to-zero* em ~5 min).
     Alternativa: **Supabase** (free, mas pausa após 1 semana ocioso) ou **Aiven** (1 GB, sem
     limite de tempo declarado).
   - **Frontend (SPA Vite):** **Cloudflare Pages** (o melhor conjunto de limites para estático).
3. **Você precisa de ≥ 512 MB de RAM**, na prática. Java 25 + Spring Boot com heap padrão
   reclama de 256 MB (ver §2). 512 MB roda; 1 GB é confortável.
4. Se o objetivo é **impressionar recrutador**, o link precisa **abrir** — logo, evite tiers
   que dormem por longos períodos, ou mitigue com *keep-alive*. Ver §4.

---

## 1. Plataformas gratuitas para container + Postgres em 2026

### 1.1 Fly.io

- **Tier gratuito:** **REMOVIDO.** A própria FAQ de preços responde "Is there a free tier?
  **No.**" Novas organizações não têm mais tier gratuito.
  Fonte: https://fly.io/docs/about/pricing/ (acesso 2026-10-21).
- **O que existe:** teste gratuito = até **2 horas** de runtime de Machine ou **7 dias**; não
  exige cartão para começar, mas **exige cartão depois**.
- **Custo mínimo real:** uma Machine `shared-cpu-1x` de **256 MB sempre ligada** sai por
  **US$ 2,19 / 30 dias**. **Postgres gerenciado Basic = US$ 38/mês.** Postgres não-gerenciado
  single-node de dev ≈ **US$ 2/mês**.
- **Dorme?** Não dorme (é uma VM/machine), mas você **paga** por ela.
- **Cartão:** obrigatório após o teste.

### 1.2 Railway

- **Tier gratuito:** existe um plano **Free permanente** de **US$ 0/mês** com **US$ 1/mês de
  crédito de uso** — mas com tetos apertados: **1 vCPU / 0,5 GB por serviço, 1 réplica, volume
  máx. 500 MB, 3 dias de logs, 0 domínios customizados, 1 projeto com 3 serviços.**
  Fonte: https://railway.com/pricing (acesso 2026-10-21).
- **Teste:** Free Trial = **US$ 5 de crédito único (30 dias)**, sem cartão.
- **Plano pago:** Hobby **US$ 5/mês** (inclui US$ 5 de uso).
- **Avaliação:** US$ 1/mês de crédito cobre pouquíssimo tempo de um Spring Boot ligado 24/7 —
  serve para demo curta ou app que dorme, não para API sempre no ar.

### 1.3 Render

- **Tier gratuito:** **Web Service Free = 512 MB RAM (0,1 CPU)**, **dorme após 15 min ocioso**,
  *spin-up* de ~1 min, **750 horas-instância grátis/mês** por workspace.
  Fontes: https://render.com/pricing e https://render.com/docs/free (acesso 2026-10-21).
- **Postgres gratuito:** 256 MB RAM / 1 GB de storage, **expira 30 dias após a criação**
  (14 dias de carência e é apagado); apenas **1 banco grátis por workspace**.
- **Dorme?** Sim (o web service). **DB expira?** Sim (30 dias).
- **Cartão:** só se você exceder o uso.
- **Avaliação:** o backend roda, mas o DB gratuito expira — **desqualifica** para portfólio de
  longo prazo, a menos que você use um Postgres gratuito de outro fornecedor (ex.: Neon).

### 1.4 Koyeb

- **Tier gratuito:** **existe.** Um Web Service grátis (Frankfurt ou Washington DC) com
  **512 MB RAM / 0,1 vCPU / 2 GB SSD**, grátis para sempre. Inclui **um PostgreSQL grátis
  limitado a 5 horas de tempo ativo + 1 GB de storage**.
  Fontes: https://www.koyeb.com/pricing e https://www.koyeb.com/docs/faqs/pricing (acesso
  2026-10-21).
- **Cartão:** **obrigatório** — há **pré-autorização de US$ 29** (hold, não cobrança).
- **Observação:** o crédito de US$ 5,50 foi **descontinuado em 1º de fevereiro de 2024**.
  Plano Pro = US$ 29/mês.
- **Avaliação:** o web service grátis é bom; o Postgres grátis de **5 horas ativas** é inviável
  para uma API em uso — combine com Neon/Supabase.

### 1.5 Northflank

- **Tier gratuito:** **Sandbox free** — "Always-on-compute – **no sleeping**", **2 serviços
  grátis, 1 banco grátis, 2 cron jobs grátis.**
  Fonte: https://www.northflank.com/pricing (acesso 2026-10-21).
- **Pago:** compute `nf-compute-20` = 0,2 vCPU / 512 MB = **US$ 5,40/mês**.
- **Avaliação:** promissor por **não dormir** e incluir **1 banco gratuito**. Os limites exatos
  de CPU/RAM do tier gratuito não estavam detalhados na página de preços — **não verificado**
  nesta sessão; confirme no painel antes de decidir.

### 1.6 Zeabur

- **Tier gratuito:** o plano **Free é só o painel** — você gerencia **seus próprios servidores**;
  **não hospeda serviços de graça**.
  Fonte: https://zeabur.com/pricing (acesso 2026-10-21).
- **Pago:** Dev **US$ 5/mês** (14 dias de teste grátis).
- **Avaliação:** descartado para hospedagem gratuita.

### 1.7 Oracle Cloud Always Free

- **Tier gratuito:** **existe e é o mais generoso do mercado.** ARM Ampere A1: **1.500
  OCPU-horas + 9.000 GB-horas/mês = 2 OCPUs / 12 GB RAM** (até 2 VMs); **2× VM.Standard.E2.1.Micro**
  (AMD, 1 GB cada); **200 GB** de block volume; **2× Autonomous AI Database** (20 GB cada);
  **10 TB/mês** de egress; 1 load balancer flexível.
  Fonte: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
  (acesso 2026-10-21).
- **Armadilhas:** instâncias **ociosas podem ser recuperadas** (CPU 95º percentil < 20%, rede
  < 20% ou memória < 20% por 7 dias) e o erro "**out of host capacity**" para A1 é comum.
- **Dorme?** Não (é VM), mas pode ser **recuperada por ociosidade**.
- **DB:** você mesmo instala Postgres na VM (não há Postgres gerenciado grátis, mas há MySQL/
  Autonomous DB grátis).
- **Avaliação:** melhor custo-benefício absoluto **se** você aceitar administrar servidor
  (patches, backup, TLS). Para um portfólio que precisa só "estar no ar", é mais trabalho do
  que Cloud Run.

### 1.8 Hugging Face Spaces

- **Mudança importante:** Spaces **Docker e Gradio agora exigem plano pago** (PRO **US$ 9/mês**
  pessoal; Team US$ 20/usuário; Enterprise US$ 50/usuário). **Static Spaces** continuam grátis
  para todos.
  Fontes: https://huggingface.co/docs/hub/spaces-overview e https://huggingface.co/pricing
  (acesso 2026-10-21).
- **Hardware CPU Basic** (2 vCPU / 16 GB) é grátis, mas **atrás do plano pago** no caso de
  Docker/Gradio. Spaces grátis **dormem** quando ociosos.
- **Avaliação:** descartado — Docker deixou de ser gratuito.

### 1.9 Google Cloud Run

- **Tier gratuito:** **existe.** Faturamento por instância: **240.000 vCPU-s + 450.000 GiB-s/mês**.
  Faturamento por requisição: **180.000 vCPU-s + 360.000 GiB-s + 2 milhões de requisições/mês**.
  **Escala a zero.**
  Fontes: https://cloud.google.com/run/pricing e
  https://cloud.google.com/free/docs/free-cloud-features (última atualização da página
  2026-10-13).
- **Cartão:** o Free Trial exige **cartão para verificação de identidade** (hold de US$ 0–1,
  não é cobrança). Exceder o free tier é cobrado a partir do cartão.
- **DB:** **não há Postgres gerenciado grátis no Cloud Run** — use Neon/Supabase/Aiven, ou
  Cloud SQL (pago).
- **Observação sobre a VM grátis:** Compute Engine always-free dá **1 `e2-micro` por mês** em
  us-west1/us-central1/us-east1 + 30 GB de disco + 1 GB de egress NA — pequeno demais para Java
  confortável, mas existe. Fonte: página de free features acima.
- **Avaliação:** **melhor opção de container grátis** para o backend, combinada com Neon.

### 1.10 AWS (Free Tier)

- **Modelo atual:** contas novas ganham **até US$ 200 em créditos** (US$ 100 imediatos + até
  US$ 100 ganhos), válidos por **6 meses**. A conta **fecha sozinha em 6 meses** ou quando os
  créditos acabam. Há serviços "always free" (30+ serviços) dentro de limites mensais.
  Fonte: https://aws.amazon.com/free/ (acesso 2026-10-21).
- **Confirmado na FAQ:** o **plano Free expira em 6 meses** (ou ao esgotar os créditos); nesse
  momento a AWS **suspende a conta** e retém os dados por **90 dias** antes de apagar tudo. Não
  é possível estender. Existem "**30+ serviços always free**" dentro de limites mensais, válidos
  "as long as you are an AWS customer". A FAQ diz que **não exige cartão** para a maioria dos
  novos clientes, mas pode pedir para verificar identidade.
  Fonte: https://aws.amazon.com/free/free-tier-faqs/ (acesso 2026-10-21).
- **Avaliação:** o ambiente de experimentação é temporário por design (6 meses); os serviços
  "always free" sobrevivem, mas o compute gratuito é modesto e o RDS free tier é historicamente
  limitado a 12 meses. Os **números exatos** de `t2.micro`/`t3.micro` e do RDS free **não foram
  verificados** nesta sessão. Para portfólio **permanente**, AWS é a pior opção gratuita de
  longo prazo.

### 1.11 Coolify e Dokploy (self-host)

- **Coolify:** **self-hosted grátis para sempre** (todos os recursos). Coolify Cloud a partir de
  **US$ 5/mês**. **Requer seu próprio servidor.**
  Fonte: https://coolify.io/pricing (acesso 2026-10-21).
- **Dokploy:** Cloud Hobby **US$ 4,50/mês por servidor** (teste 7 dias, sem cartão); também
  auto-hospedável. **Requer seu próprio servidor.**
  Fonte: https://dokploy.com/pricing (acesso 2026-10-21).
- **Avaliação:** são ótimos **se** você tiver uma VM (ex.: Oracle Always Free) e quiser um
  PaaS próprio. Não geram hospedagem grátis por si só.

---

## 2. Spring Boot 4 / Java 25: quanto de RAM, virtual threads e GraalVM native

### 2.1 Versões e requisitos

- Spring Boot **4.0.0** foi lançado em **20/11/2025**; a linha estável mais recente é **4.1.1**
  (a mesma do `build.gradle` deste repositório).
  Fonte: https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now (acesso 2026-10-21).
- Requisitos: **Java 17+**, compatível até **Java 26**; GraalVM native exige **GraalVM 25+**
  (Community 25) e Native Build Tools 0.11.5.
  Fonte: https://docs.spring.io/spring-boot/4.0/system-requirements.html (acesso 2026-10-21).
- Spring Boot 4 traz suporte first-class a **Java 25**, modularização, Jackson 3.0, Tomcat 11.0,
  Jakarta Servlet 6.1.
  Fonte: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes
  (acesso 2026-10-21).

### 2.2 Heap padrão do JVM — o número que decide tudo

O JVM HotSpot escolhe o **heap máximo = 1/4 da memória física** (e heap inicial = 1/64).
Fonte: https://docs.oracle.com/en/java/javase/25/gctuning/ergonomics.html (acesso 2026-10-21) —
"Initial heap size of 1/64 of physical memory; Maximum heap size of 1/4 of physical memory".
Confirmado também em
https://docs.oracle.com/en/java/javase/25/gctuning/parallel-collector1.html.

Em ambiente conteinerizado, o JVM (desde o Java 10) é *container-aware* e usa o **limite do
container** como "memória física". Consequência prática:

| Limite do container | Heap máx. padrão | Veredito |
|---|---|---|
| **256 MB** (Fly/Railway) | ~64 MB | **Insuficiente** para Spring Boot + JPA/Hibernate. OOM provável. |
| **512 MB** (Render/Koyeb/Northflank) | ~128 MB | **Roda** uma API MVC enxuta; aperte `-XX:MaxRAMPercentage` e limite metadados. |
| **1 GB** | ~256 MB | **Confortável.** |
| **2 GB+** | ~512 MB+ | Folgado. |

> O JVM *container-aware* foi introduzido em **JDK 10 (JEP 300 / UseContainerSupport)** e é padrão
> desde então. **Não re-verifiquei a URL canônica do JEP nesta sessão** — trate a afirmação
> "heap = 1/4 do limite do container" como derivada da doc de ergonomia acima, que é primária.

### 2.3 Virtual threads (Project Loom)

- Virtual threads são `java.lang.Thread` **leves**, com **M:N** scheduling sobre poucas threads
  de SO. São ideais para cargas **I/O-bound** (o seu caso: uma API que fala com Postgres).
  Fonte: https://openjdk.org/jeps/444 (JEP 444, *Virtual Threads*, entregue no JDK 21; página
  atualizada 2025-10-30) (acesso 2026-10-21).
- **Memória:** o JEP diz que "the heap consumption and garbage collector activity of
  thread-per-request versus asynchronous code should be **roughly similar**". Ou seja:
  **virtual threads NÃO reduzem significativamente o consumo de RAM** de uma API pequena — eles
  reduzem o **custo por thread** e aumentam throughput, não o *footprint* base.
- **Importante:** virtual threads **não tornam o Spring Boot menor** para caber em 256 MB. O
  custo base (JVM + Spring context + Hibernate + drivers) não muda.
- **Como ligar:** `spring.threads.virtual.enabled=true` (suportado no Spring Boot; mencionado na
  doc de native image citada em §2.4).

### 2.4 GraalVM Native Image (a arma real contra pouca RAM)

- Native images "can run with a **smaller memory footprint** and with much faster startup times"
  e são "especially interesting when combined with **Function as a service (FaaS)** platforms".
  Fonte: https://docs.spring.io/spring-boot/4.0/reference/packaging/native-image/introducing-graalvm-native-images.html
  (acesso 2026-10-21).
- **Maturidade 2026:** suporte first-class no Spring Boot 4 com **GraalVM 25+**; AOT processing
  estável. **Mas** há um *closed-world*: `@Profile` tem limitações, `@ConditionalOnProperty` e
  propriedades `.enabled` **não** funcionam como antes, classpath fixo, **sem lazy loading**.
  Fontes: a mesma URL acima + https://docs.spring.io/spring-boot/4.0/system-requirements.html.
- **Impacto no seu projeto:** o seu boot falha sem as duas chaves LGPD — isso é uma
  `@ConfigurationProperties`/validação; funciona em native, mas **teste** o build nativo, porque
  reflexão/Jackson 3 podem exigir hints.
- **Ganho típico esperado:** de ~150–300 MB (JVM) para **~50–100 MB** de RSS, e startup de
  segundos para **decenas de ms**. Os números exatos **não constam nas páginas oficiais citadas**
  — são estimativas comuns, **não verificadas** aqui. O que **está** documentado é a direção
  ("smaller memory footprint", "much faster startup").
- **Recomendação:** se você quiser caber num tier de **256–512 MB**, native image (ou pelo menos
  `-XX:MaxRAMPercentage=50 -XX:+UseSerialGC`) é o caminho.

### 2.5 Empacotamento eficiente

- Spring Boot recomenda **desempacotar o jar** (`java -Djarmode=tools -jar app.jar extract`) para
  startup mais rápido em produção (o layout é *AOT cache / CDS friendly*).
  Fonte: https://docs.spring.io/spring-boot/4.0/reference/packaging/efficient.html (acesso
  2026-10-21).
- Use **CDS/AOT cache** (`-XX:AOTCache`) para cortar tempo e memória de startup. Fonte:
  https://docs.spring.io/spring-boot/4.0/reference/packaging/aot-cache.html (não aberta nesta
  sessão — **não verificada em detalhe**).

---

## 3. Postgres gerenciado gratuito

| Fornecedor | Free hoje? | Limites principais | Dorme / expira? | Cartão? |
|---|---|---|---|---|
| **Neon** | Sim, **permanente** | 100 CU-horas/projeto; autoscaling até 2 CU (8 GB RAM); 1 GB/projeto + 20 GB/conta; 5 GB egress; 6 h de history | *Scale-to-zero* após **5 min ociosos** (fixo no Free, **não desativável**); reativa em "**algumas centenas de ms**" | **Não** |
| **Supabase** | Sim | 500 MB DB; CPU compartilhada / 500 MB RAM; 5 GB egress; 1 GB storage; 50k MAU; máx. 2 projetos ativos | **Pausa após 1 semana de inatividade** | Não |
| **Aiven** | Sim | 1 VM dedicada, 1 CPU, **1 GB RAM**, 1 GB storage, regiões limitadas, sem connection pooling, 1 backup | "**Free plans do not have any time limitations**", mas a Aiven pode desligar serviços **não usados por período prolongado** | Não (típico) |
| **Turso** | Sim | 100 DBs; 5 GB storage; 500M linhas lidas; 10M linhas escritas/mês; 1 dia de PITR | Não | Não |
| **Xata** | **Não** | Só self-host ou trial de 14 dias; Cloud pay-as-you-go (micro ≤2 vCPU/1 GB = **US$ 9/mês**) | — | Sim (Cloud) |

**Fontes:** https://neon.com/pricing e https://neon.com/docs/introduction/scale-to-zero (Neon);
https://supabase.com/pricing (Supabase); https://aiven.io/pricing (Aiven);
https://turso.tech/pricing (Turso); https://xata.io/pricing (Xata). Acesso 2026-10-21.

**Atenção de compatibilidade:**

- **Turso é SQLite/libSQL, NÃO Postgres.** Seu stack usa `org.postgresql:postgresql` e JPA/
  Hibernate com dialeto Postgres — migrar para Turso é mudança de arquitetura, **não** um
  *drop-in replacement*. Descartado para este projeto.
- **Neon agora é parte da Databricks** ("Lakebase Postgres") — a página de preços continua com
  tier Free. Fonte: https://neon.com/pricing.
- **`neon.com/faqs/free-plan` retornou 404** nesta sessão; não foi possível ler a FAQ específica
  do plano Free. Os números acima vêm da página de preços e da doc de scale-to-zero.

**Escolha recomendada:** **Neon** — free permanente, sem cartão, o *scale-to-zero* de ~5 min
causa no máximo um cold start de sub-segundo no banco (bem melhor que a pausa de 1 semana do
Supabase). Se você quer evitar qualquer suspensão, **Aiven** (1 GB) é a alternativa sem limite
de tempo declarado.

---

## 4. Alternativas a hospedar o backend

### 4.1 Grátis (o que você tem hoje)

| Opção combinada | Backend | Banco | Prós | Contras |
|---|---|---|---|---|
| **A** | Cloud Run (container) | Neon | Scale-to-zero → US$ 0 real; sem cartão cobrado dentro do free; escala | *Cold start* de segundos; precisa de Dockerfile enxuto |
| **B** | Oracle Always Free VM | Postgres na própria VM | 2 OCPU/12 GB ARM; sempre ligado | Você administra tudo; "out of host capacity"; risco de reciclagem por ociosidade |
| **C** | Koyeb web service (512 MB) | Neon/Aiven | Grátis para sempre; sem dormir | Exige cartão (hold de US$ 29); Postgres grátis só 5 h ativas |
| **D** | Northflank Sandbox | Neon | "**No sleeping**"; 1 banco grátis | Limites de CPU/RAM do free **não verificados**; pode mudar |
| **E** | Render Free (512 MB) | Neon | Sem cartão no free | Dorme após 15 min; ~1 min de spin-up |
| **F** | Fly.io / Railway / Zeabur / HF Docker | — | — | **Pagos / tier grátis removido / Docker agora pago.** Descartados |

### 4.2 A ~US$ 5/mês (quando US$ 0 atrapalha mais que ajuda)

- **Railway Hobby US$ 5/mês** (inclui US$ 5 de uso) — sem dormir, Postgres fácil.
- **Fly.io** `shared-cpu-1x` 256 MB ≈ **US$ 2,19/30 dias** por Machine (+ Postgres ~US$ 2/mês
  não-gerenciado).
- **Northflank** `nf-compute-20` (0,2 vCPU/512 MB) = **US$ 5,40/mês**.
- **Coolify Cloud US$ 5/mês** + sua VM; **Dokploy Cloud US$ 4,50/mês** por servidor.
- **Aiven Developer US$ 5/mês**; **Zeabur Dev US$ 5/mês**.

Fontes: páginas de preços já citadas em §1.

### 4.3 Só local (sem hospedar)

- Rode com `docker compose up --build` (é o que este repositório já faz) e grave **vídeo/GIF do
  fluxo funcionando** + README com `curl` de exemplos.
- **Contras:** o recrutador não consegue "clicar e testar" na hora.

### 4.4 O que o recrutador espera (e a evidência disponível)

> **Honestidade de fonte:** **não encontrei**, nesta sessão, um artigo primário/estudo
> autoritativo que quantifique "quanto tempo de cold start um recrutador tolera" nem "o link
> precisa estar no ar". As tentativas (`stackoverflow.blog`, `freecodecamp`) retornaram 404.
> O que segue é **opinião fundamentada no senso comum da indústria**, **não um dado verificado** —
> trate como heurística, não como fato citável.

Heurísticas geralmente aceitas:

- **O repositório pesa mais que a demo.** README claro, testes verdes, histórico de commits,
  arquitetura explicada (o seu `GLOSSARY.md` + ADRs são exatamente esse tipo de sinal — e são
  **fortes**).
- **Um link "morto" ou que demora 1 minuto para subir é pior que nenhum link.** Se o backend
  dorme (Render, Railway free, Koyeb Postgres), ou o link abre ou não. Prefira tiers que **não
  dormem** (Cloud Run com *min-instances*=0 mas reativo; Northflank "no sleeping"; Oracle VM) ou
  adicione um *uptime ping* (cron gratuito batendo a cada 10 min) para manter quente.
- **Tolerância a cold start:** ~1–3 s é aceitável; ~30–60 s (Render spin-up, Supabase pausado)
  costuma levar o visitante a desistir. **Não medido; estimativa.**
- **Native image** elimina quase todo o cold start do lado Java (documentado: "much faster
  startup times") e é, por si só, um **diferencial de portfólio** — mostra que você entende
  deploy, não só `@RestController`.

**Recomendação de portfólio:** hospede em **Cloud Run + Neon** com **GraalVM native image**, escreva
no README "a primeira requisição pode levar ~1–2 s (cold start)", e mantenha o **vídeo de
demonstração** como plano B se o free tier mudar.

---

## 5. Frontend grátis para SPA Vite (React/Vue)

| Plataforma | Plano grátis | Limites | Veredito |
|---|---|---|---|
| **Cloudflare Pages** | Sim | 1 build concorrente; 500 builds/mês; 100 domínios/projeto; 20.000 arquivos/site; 25 MiB/arquivo; requisições de asset **grátis e ilimitadas**; Functions contam no Workers Free (**100.000 req/dia**) | **Melhor escolha** para estático |
| **Vercel** | Hobby US$ 0 | 100 GB transfer; 1M req CDN; 1M invocações de função; Fluid Active CPU 4 h/mês; 100 GB-h memória | Ótimo; Hobby é **só uso pessoal não-comercial**; sem compra extra de uso |
| **Netlify** | Free US$ 0 para sempre | Modelo **por créditos: teto de 300**; deploy de produção = 15 créditos; banda = 20 créditos/GB; compute = 10 créditos/GB-h; web requests = 2 créditos/10k | Mudou para créditos; cuide do teto |
| **GitHub Pages** | Sim | Estático puro; **1 site/conta (user/org)** ou **1/repo (project)**; domínio custom suportado | Simples e estável; os números "soft" (1 GB site / 100 GB-mês / 10 builds/h) **não constam** na página oficial aberta — **não verificados** |

**Fontes:** https://developers.cloudflare.com/pages/platform/limits/ e
https://developers.cloudflare.com/pages/functions/pricing/ (Cloudflare);
https://vercel.com/pricing (Vercel); https://www.netlify.com/pricing (Netlify);
https://docs.github.com/en/pages/getting-started-with-github-pages/about-github-pages
(GitHub Pages — confirma "maximum of one pages site per account" e "per repository", e suporte a
domínio custom). Acesso 2026-10-21.

**Nota Vite/SPA:** todos suportam SPA — configure o *rewrite* de fallback para `index.html`
(no Cloudflare Pages, o arquivo `_redirects` com `/* /index.html 200`). Isso **não** foi
verificado em doc nesta sessão para cada plataforma, mas é comportamento padrão documentado
delas.

---

## 6. Tabela comparativa consolidada

### Backend (container)

| Plataforma | Grátis hoje? | RAM grátis | Dorme? | Cartão? | Nota |
|---|---|---|---|---|---|
| **Google Cloud Run** | Sim | conforme req. (240k vCPU-s/450k GiB-s) | Scale-to-zero | Só p/ verificação | ⭐ **Melhor container grátis** |
| **Oracle Always Free** | Sim | 12 GB ARM (2 OCPU) | Não | Sim (verificação) | ⭐ Melhor se admin OK; risco de reciclagem |
| **Koyeb** | Sim | 512 MB / 0,1 vCPU | Não | **Sim (hold US$ 29)** | Bom; DB grátis inútil (5 h) |
| **Northflank** | Sim | **não verificado** | **Não** | Não (típico) | Promissor; confirmar limites |
| **Render** | Sim | 512 MB / 0,1 CPU | **Sim, 15 min** | Só se exceder | Spin-up ~1 min |
| **Railway** | Sim (US$ 1/mês uso) | 0,5 GB/serviço | Não (mas estoura crédito) | Não | Crédito baixo p/ 24/7 |
| **Hugging Face** | **Não p/ Docker** | — | — | — | Docker agora exige PRO US$ 9 |
| **Fly.io** | **REMOVIDO** | — | — | Sim após trial | Mín. US$ 2,19/30 d |
| **Zeabur** | **Não hospeda** | — | — | — | Só painel |
| **Xata** | **Não** | — | — | Sim | Trial 14 dias |

### Banco (Postgres)

| Fornecedor | Grátis | Storage | Dorme/expira | Cartão | Nota |
|---|---|---|---|---|---|
| **Neon** | Permanente | 1 GB/proj | Scale-to-zero 5 min | Não | ⭐ **Melhor** |
| **Supabase** | Sim | 500 MB | **Pausa 1 semana** | Não | 2 projetos ativos |
| **Aiven** | Sim | 1 GB | Sem limite declarado | Não | Alternativa estável |
| **Render** | Sim | 1 GB | **Expira em 30 dias** | Não | Descartado p/ longo prazo |
| **Turso** | Sim | 5 GB | Não | Não | **SQLite, não Postgres** |
| **Xata** | Não | — | — | Sim | Pay-as-you-go US$ 9 |

### Frontend (SPA)

| Plataforma | Grátis | Banda/req | Cartão | Nota |
|---|---|---|---|---|
| **Cloudflare Pages** | Sim | Assets **ilimitados e grátis** | Não | ⭐ **Melhor** |
| **Vercel** | Hobby | 100 GB / 1M CDN | Não | Pessoal não-comercial |
| **Netlify** | Free | **300 créditos** | Não | Modelo por créditos |
| **GitHub Pages** | Sim | soft (1 GB/100 GB) | Não | Simples |

---

## 7. Recomendação final

Para **este projeto** (Spring Boot 4.1.1 / Java 25 em Docker + Postgres 16 + SPA Vite), com custo
**US$ 0** e foco em **portfólio**:

### Receita principal (grátis, sem dormir por muito tempo)

1. **Backend → Google Cloud Run** (container Docker já existente), com
   `--min-instances=0 --max-instances=1 --memory=512Mi` e
   `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC`. O Cloud Run escala a zero e cobra **US$ 0**
   dentro de 180k vCPU-s / 360k GiB-s / 2M req por mês.
2. **Banco → Neon** (free permanente, sem cartão, Postgres de verdade). Cold start de banco
   < 1 s.
3. **Frontend → Cloudflare Pages** (assets grátis e ilimitados; 500 builds/mês).
4. **Diferencial:** compile o backend como **GraalVM native image** (GraalVM 25+, Spring Boot 4
   suporta) para cortar RSS e cold start. Teste as duas chaves LGPD no build nativo.

### Plano B (se você quiser algo sempre quente e sem cold start)

- **Oracle Cloud Always Free**: 1 VM ARM (2 OCPU / 12 GB) rodando **Postgres + a API** via
  `docker compose`. Configure keep-alive/ping para **não** ser reciclada por ociosidade e
  monitore o erro "out of host capacity" na criação.

### O que NÃO usar

- **Fly.io** (free tier removido), **Hugging Face Docker** (agora pago), **Zeabur** (não
  hospeda), **Xata** (sem free), **Render Postgres** (expira em 30 dias), **Turso** (não é
  Postgres).

### Sobre "grátis" que na prática custa

Se US$ 0 te faz perder tempo com cold starts e DBs que expiram, **Railway Hobby (US$ 5/mês)** ou
**Fly.io (~US$ 2–4/mês)** resolvem com folga — mas para **portfólio**, a receita gratuita acima é
suficiente e não compromete a demo.

---

## Apêndice A — Itens não verificados nesta sessão (transparência)

| Afirmação | Status |
|---|---|
| Heurística de "quanto o recrutador tolera de cold start" | **Não verificada** — opinião, sem fonte primária |
| "Native image cai para ~50–100 MB de RSS" (números exatos) | **Não verificado** — só a direção está documentada |
| Limites exatos de CPU/RAM do free tier da Northflank | **Não verificado** — página de preços não detalha |
| Números exatos de `t2.micro`/`t3.micro` no "always free" da AWS | **Não verificados** — a FAQ confirma que existem "30+ always-free services", sem detalhar compute |
| Limites "soft" do GitHub Pages (1 GB, 100 GB/mês, 10 builds/h) | **Não verificados** — a página oficial aberta só confirma "1 site/conta" e "1/repo", **não** os números |
| URL canônica do JEP de container-awareness (JDK 10) | **Não re-verificada** |
| `_redirects` de SPA em cada plataforma de frontend | **Não verificado** em doc, por plataforma |
| Página `neon.com/faqs/free-plan` | **404** nesta sessão |

## Apêndice B — Índice de fontes (todas acessadas em 2026-10-21)

**Hospedagem de container / PaaS**
- Fly.io: https://fly.io/docs/about/pricing/
- Railway: https://railway.com/pricing
- Render: https://render.com/pricing · https://render.com/docs/free
- Koyeb: https://www.koyeb.com/pricing · https://www.koyeb.com/docs/faqs/pricing
- Northflank: https://www.northflank.com/pricing
- Zeabur: https://zeabur.com/pricing
- Hugging Face: https://huggingface.co/docs/hub/spaces-overview · https://huggingface.co/pricing
- Coolify: https://coolify.io/pricing
- Dokploy: https://dokploy.com/pricing

**Nuvens gerais**
- Oracle Always Free: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm
- Google Cloud Run: https://cloud.google.com/run/pricing
- Google Free Tier: https://cloud.google.com/free/docs/free-cloud-features (atualizada 2026-10-13)
- AWS Free Tier: https://aws.amazon.com/free/

**Banco de dados gerenciado**
- Supabase: https://supabase.com/pricing
- Neon: https://neon.com/pricing · https://neon.com/docs/introduction/scale-to-zero
- Turso: https://turso.tech/pricing
- Xata: https://xata.io/pricing
- Aiven: https://aiven.io/pricing

**Frontend**
- Vercel: https://vercel.com/pricing
- Netlify: https://www.netlify.com/pricing
- Cloudflare Pages: https://developers.cloudflare.com/pages/platform/limits/ ·
  https://developers.cloudflare.com/pages/functions/pricing/
- GitHub Pages: https://docs.github.com/en/pages/getting-started-with-github-pages/about-github-pages

**Java / Spring Boot**
- Spring Boot 4.0 release: https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now
- System Requirements: https://docs.spring.io/spring-boot/4.0/system-requirements.html
- GraalVM Native Images: https://docs.spring.io/spring-boot/4.0/reference/packaging/native-image/introducing-graalvm-native-images.html
- Efficient Deployments: https://docs.spring.io/spring-boot/4.0/reference/packaging/efficient.html
- Release Notes 4.0: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes
- JEP 444 Virtual Threads: https://openjdk.org/jeps/444
- JVM Ergonomics (heap default): https://docs.oracle.com/en/java/javase/25/gctuning/ergonomics.html
- Parallel Collector (heap default): https://docs.oracle.com/en/java/javase/25/gctuning/parallel-collector1.html
