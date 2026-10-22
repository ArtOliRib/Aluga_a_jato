# Como um projeto de portfólio backend Java/Spring Boot é avaliado (2026)

**Escopo:** avaliação de repositório de portfólio por recrutadores técnicos e tech leads, com foco em **documentação e legibilidade**. Contexto: desenvolvedor solo, API de aluguel de carros, Spring Boot 4 / Java 25 / PostgreSQL. Foco no repositório, não em manter deploy no ar.

**Nota metodológica (leia antes):** grande parte do material público sobre "como avaliar portfólio" vem de blogs de empresas de recrutamento, bootcamps e ferramentas de currículo — ou seja, **conteúdo com interesse comercial**, não pesquisa neutra. Onde tentei acessar fontes primárias de pesquisa, algumas foram bloqueadas (Reddit e ResearchGate retornaram 403/erro de renderização). Marquei cada afirmação com o nível de confiança: **[PRIMÁRIA]** = pesquisa acadêmica ou fonte institucional; **[SECUNDÁRIA]** = blog/empresa com interesse; **[OPINIÃO]** = voz individual, não consenso.

---

## 1. O que recrutadores/tech leads fazem ao abrir um repositório em 2026

**Não encontrei estudo de eye-tracking que meça especificamente a proporção de atenção README vs. código em triagem de candidatos.** Isso parece não existir publicamente. Os números que circulam são estimativas de blogs, não medições. Seja cético com eles.

O que consegui verificar, com graus variados de solidez:

### Os números de "tempo de atenção"

| Fonte | Alegação | Data | Confiabilidade |
|---|---|---|---|
| applyfinch.com | ~30 s no perfil do GitHub; **"primeiros 5 s: foto/bio; próximos 10 s: repos fixados; últimos 15 s: entra num repo, lê o README, olha a barra de linguagens e a data do último commit"** | Publicado mar/2026, atualizado out/2026 | **[SECUNDÁRIA]** — é uma ferramenta de currículo (applyfinch); tem interesse em inflar a importância do GitHub/README. Mas o detalhamento do funil de 30 s é o mais granular que encontrei. |
| onehour.digital | "8,2 s em média revisando portfólios"; "55 s cobre avaliação de currículo + portfólio em 2026" | 2026 | **[SECUNDÁRIA]** — agência de marketing; números não rastreáveis a metodologia. |
| priygop.com | "90 s escaneando seu GitHub" | 2026 | **[SECUNDÁRIA]** — contradiz os 30 s de outras fontes; ilustra a inconsistência dos números. |
| flatironschool.com | "recrutadores gastam ~6 s por currículo no scan inicial" | s/d | **[SECUNDÁRIA]** — bootcamp. |

**Conclusão cética:** o número exato (6 s? 30 s? 90 s?) **não é verificável** e as fontes se contradizem. O que é consistente entre todas: **a janela é curta, é uma varredura (scan), e o README é o artefato mais lido dentro dessa janela.** Trate "30 s" como ordem de grandeza, não como dado.

### O que faz fechar a aba

- **applyfinch [SECUNDÁRIA], Mar/2026:** "Ninguém está lendo seu código. Ninguém está checando cobertura de testes. Ninguém se importa com o gráfico de contribuições verde todo dia." E, no lado do que *derruba*: "repos sem README ou com o cabeçalho padrão 'Project Title'" e "um GitHub vazio ou bagunçado é pior que nenhum GitHub". → https://applyfinch.com/blog/github-on-resume
- **Nitin Aggarwal (LinkedIn) [OPINIÃO], ~2025:** "Eu checo o perfil do GitHub de todo candidato. Na maioria deles eu fecho em segundos." → https://www.linkedin.com/posts/nitinagam_i-check-every-candidates-github-profile-activity-7505815292065931265-339H (não consegui verificar o texto completo por ser LinkedIn; o trecho vem do resultado de busca).
- **David Chung (LinkedIn) [OPINIÃO]:** "Se eles *olham* o GitHub, gastam 30 segundos — e procuram uma coisa: você entrega?" / "um repo cheio de side projects pela metade..." → https://www.linkedin.com/posts/davidkchung_github-is-a-gold-mine-for-technical-recruiters-activity-7473754020046798848-Dy3S

### Evidência mais robusta sobre *o que* o avaliador procura

- **SOLTECH (empresa de staffing/consultoria), out/2026 [SECUNDÁRIA, mas institucional]:** guia para RH sobre avaliar engenheiros. Recomenda explicitamente olhar no portfólio: *"o problema que o engenheiro estava resolvendo, por que escolheu uma abordagem, os trade-offs que fez e o que aprendeu"* — e não a complexidade do projeto. Também: *"Comunicação é frequentemente categorizada como soft skill, mas em engenharia de software está ligada à efetividade técnica. Engenheiros precisam ... documentar decisões, explicar limitações e comunicar trade-offs."* → https://soltech.net/how-do-hiring-managers-actually-look-for-in-a-github-portfolio/ (título original: "How to Evaluate Software Engineering Talent")
- **Hyperskill (plataforma de ensino JetBrains) [SECUNDÁRIA, com interesse], jun/2026:** afirma que o primeiro julgamento é *"se você construiu ou seguiu um tutorial"*, com assinaturas reconhecíveis: "o README descreve o que o projeto faz em vez de por que decisões foram tomadas". E a frase-chave: *"O portfólio que recebe atenção não é o mais impressionante. É o que se sustenta quando alguém que sabe o que está olhando passa trinta minutos com ele."* → https://hyperskill.org/blog/post/building-a-developer-portfolio-in-2026-what-actually-gets-attention

**Resumo da seção 1:** a janela inicial é curta e o README domina; mas o que *decide a contratação de fato* é a conversa de 20–30 min sobre o projeto (o que ele resolve, quais decisões e trade-offs). O README é o **gatilho** dessa conversa, não o veredito.

---

## 2. Estrutura de README que funciona para backend

### Há consenso sobre a ordem? Parcial.

**Consenso forte** (todas as fontes convergem): os primeiros parágrafos devem responder **o que é e por que existe / qual problema resolve**, antes de qualquer parte técnica.

- **makeareadme.com [SECUNDÁRIA, referência comunitária amplamente usada]:** seção "Description" vem logo após o nome; "deixe as pessoas saberem o que seu projeto pode fazer especificamente". Curiosamente o template dele também coloca **Installation antes de Usage**. → https://www.makeareadme.com/
- **freeCodeCamp / Atomic Spin [SECUNDÁRIA]:** "descrição breve do repositório ... como ele se relaciona à aplicação maior". → https://spin.atomicobject.com/valuable-readme/
- **Hyperskill [SECUNDÁRIA], jun/2026:** "A maioria dos READMEs de portfólio descreve o que o projeto é. Um bom README explica **que problema resolve**, como rodar a partir de um clone limpo, as decisões arquiteturais-chave e por quê, e o que o desenvolvedor mudaria numa próxima iteração." → https://hyperskill.org/blog/post/building-a-developer-portfolio-in-2026-what-actually-gets-attention
- **Superdev Academy [SECUNDÁRIA]:** "README com pelo menos 6 partes: **o problema que resolve**, um screenshot/demo, como rodar, **as razões por trás das decisões-chave**..." → https://www.superdevacademy.com/en/blogs/portfolio-programmer-junior-first-job-ai-era
- **LinkedIn "top content" / Sefism [SECUNDÁRIA]:** "What it is (1–2 frases); **Why: o problema que resolve e para quem**; How to run". → https://www.sefism.com/questions/how-to-build-a-developer-portfolio-as-a-beginner/

**Ponto onde as fontes divergem:** "como rodar" logo cedo (makeareadme) vs. depois do problema/decisões (Hyperskill, Superdev). Não há norma única — é **opinião/framework de cada fonte**, não consenso.

### Onde entra "o problema que resolve" vs. "como rodar"

Padrão emergente mais repetido (e alinhado ao seu objetivo — primeira metade direta, técnica depois):
1. Nome + uma frase de valor (o "gancho").
2. O problema que resolve e para quem.
3. Visual/demo (se houver UI ou CLI) — **logo após o gancho**, antes da lista de features.
4. Como rodar (fresh clone → funcionando).
5. Decisões arquiteturais e trade-offs (aqui cabe Spring Boot/Java 25/PostgreSQL e o *porquê*).
6. Testes e limitações.
7. Detalhes técnicos profundos / link para `/docs`.

### Críticas a READMEs longos demais

- **Ofer Shapira (DEV), mar/2026 [OPINIÃO de mantenedor, 40+ projetos OSS]:** "O README de 500 linhas: se seu README precisa de um sumário, a maior parte deveria estar em `/docs`. **O README é uma landing page, não um manual.**" Também: "Desenvolvedores não leem READMEs. Eles **escaneiam**. Parágrafos longos são pulados inteiros. Mantenha parágrafos com menos de 3 frases." → https://dev.to/ofershap/8-readme-mistakes-killing-your-github-stars-and-how-to-fix-them-ong
- **makeareadme [SECUNDÁRIA]** — contraponto relevante: "enquanto um README pode ser longo e detalhado demais, **longo demais é melhor que curto demais**. Se achar seu README longo, considere usar outra forma de documentação em vez de cortar informação." → https://www.makeareadme.com/

**Resumo da seção 2:** o consenso real é **"gancho/problema primeiro"**, e o resto da ordem é negociável. "Como rodar" perto do topo ajuda quem quer executar; se o seu objetivo é leitura/legibilidade (não deploy), problema → decisões → como rodar é defensável e é o que a Hyperskill e a Superdev descrevem. A crítica a README longo é consistente, mas o contraponto é: **corte movendo para `/docs`, não apagando informação.**

---

## 3. Comentários em código: o que é bom em 2026

### "Comente o porquê, não o quê" — ainda vale?

**Sim, continua sendo o consenso.** Aparece em fontes de 2026 e em material didático institucional.

- **HN (thread "My agent.md..."), ago/2026 [OPINIÃO de practitioner]:** lista "Comment why, not what. Document public APIs. Readability is paramount." → https://news.ycombinator.com/item?id=49410932
- **Slides didáticos (KIT, Alemanha) [SECUNDÁRIA]:** "Comment why, not what the code does. Keep comments close. **Avoid outdated comments — update as code changes.**" → https://indico.kit.edu/event/5258/sessions/4921/attachments/9676/16392/DocumentationBasics.pdf
- **Reddit r/ProgrammerHumor, 2026 [OPINIÃO]:** "Comente 'porquê' não 'o quê', é tudo que você precisa se não for óbvio." → https://www.reddit.com/r/ProgrammerHumor/comments/1wgllyu/

### O que a pesquisa acadêmica diz (aqui está o dado mais forte, e ele nuança a regra)

**Abdelsalam, Peitek, Bergum & Apel, "The Effect of Comments on Program Comprehension: An Eye-tracking Study", Empirical Software Engineering, 2026 [PRIMÁRIA, peer-reviewed].**
- Achado central: **o efeito dos comentários varia muito conforme o trecho de código**, "de uma queda de 30% a um aumento de 34% no desempenho". Ou seja: **comentar nem sempre ajuda.** Comentários que só repetem o código não melhoram compreensão; o ganho vem quando o comentário adiciona contexto que o código não expressa.
- A conclusão do estudo é que o efeito "varia significativamente", então não existe "comente sempre" universal.
- Link (abstract): https://link.springer.com/article/10.1007/s10664-025-10721-2 · DOI: https://dl.acm.org/doi/10.1007/s10664-025-10721-2
- **Atenção:** não consegui abrir o PDF completo (Springer bloqueou o carregamento). Os números "30%/34%" vêm do abstract/resumos de indexação (ResearchGate, ACM, Semantic Scholar), não de leitura direta do estudo. Trate como **bem corroborado, mas não lido na íntegra**.

- **Peitek, Hess & Apel, "From Restructuring to Stabilization..." (TOSEM), fev/2026 [PRIMÁRIA]:** experimento com GPT-5.1 em 230 trechos Java mostrou que o refactoring "podou comentários" (comment pruning) numa fase inicial — e os autores alertam que as ferramentas devem ter *"mecanismos para preservar comentários explicativos valiosos (para humanos)"*. Ou seja: **a comunidade de pesquisa reconhece que comentários explicativos são um ativo que vale proteger.** → https://arxiv.org/html/2602.21833v1

### Javadoc em Java — como avaliadores julgam

- **Sonar (SonarSource), jul/2025 [SECUNDÁRIA, mas ferramenta de qualidade usada em indústria]:** Java 23 introduziu **comentários de documentação em Markdown** (`///`), e o SonarQube criou regras (S7476, S7474) para padronizar. Recomendação: **não misturar** Markdown com tags HTML/Javadoc legadas; usar Markdown de forma consistente. Implicação prática: **Javadoc continua relevante em 2026**, mas o que se cobra é *consistência e utilidade*, não "Javadoc em todo método". → https://www.sonarsource.com/blog/java-23-embrace-the-new-era-of-code-comments/
- **InfoWorld, "More Effective Javadoc" [SECUNDÁRIA]:** "Javadoc inútil ... pode ser prejudicial se ficar obsoleto e for superado pelas mudanças no código." → https://www.infoworld.com/article/2162395/more-effective-javadoc.html
- **Oracle, guia oficial do Javadoc [PRIMÁRIA/institucional]:** boas práticas (ex.: documentar exceções onde apropriado). → https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html
- **Reddit r/java [OPINIÃO]:** "Código deve ser compreensível o suficiente para deixar claro O QUE faz. Esse tipo de comentário é inútil e até prejudicial à legibilidade." → https://www.reddit.com/r/java/comments/ncs7io/do_we_need_comments_in_our_code/

### O que se espera de um projeto "didático"

Se você *declara* o projeto como didático/portfólio, o padrão sobe: espera-se que os comentários expliquem **decisões de design e trade-offs**, não sintaxe. É exatamente o que SOLTECH (out/2026) diz que avaliadores procuram — "por que escolheu essa abordagem, os trade-offs". Um comentário `// Cria novo usuário` num `new User()` é apontado como antipadrão em quase todas as fontes de 2025–2026.

**Resumo da seção 3:** "porquê, não o quê" segue válido e é consenso. A novidade de 2026 é a evidência empírica de que **comentário que só repete o código é neutro-a-negativo**, e que comentário explicativo vale ouro. A regra prática: *se remover o comentário não perde informação, remova.*

---

## 4. ADRs em portfólio: impressiona ou é over-engineering?

**Não encontrei nenhuma fonte que diga diretamente "tech leads avaliam ADRs em portfólio de candidato".** Essa ligação específica **não é verificável** com o que encontrei. Fui honesto: é um ponto cego. O que consigo sustentar é evidência indireta.

- **Red Hat [SECUNDÁRIA, institucional]:** "Usar ADR para **projetos pequenos pode ser overkill**, mas quando o projeto mira múltiplos stakeholders... torna-se uma ferramenta poderosa." → https://www.redhat.com/en/blog/architecture-decision-records — **este é o ponto mais direto que encontrei e ele sugere cautela para projeto solo pequeno.**
- **Spotify Engineering, abr/2020 [PRIMÁRIA/institucional, mas DATADA]:** "Escreva um ADR quase sempre" — inclusive para decisões pequenas, pois "ADRs podem ser leves". Contexto: é sobre **times**, não portfólio individual. É de **2020 e reflete escala de empresa grande**; aplicá-lo a um projeto solo é extrapolação. → https://engineering.atspotify.com/2020/04/when-should-i-write-an-architecture-decision-record
- **Niels.nu [OPINIÃO, 2018 — DATADO]:** "parece bom no currículo, mas não sinto que arquiteto de software seja um título de função." → https://niels.nu/blog/2018/lightweight-architecture-decision-records/

**Leitura cética e útil para o seu caso:** o *valor* de um ADR em portfólio não é o formato em si — é o **substituto textual do "por que você escolheu X" que o entrevistador vai perguntar na conversa**. SOLTECH e Hyperskill (ambas 2026) dizem que avaliadores querem exatamente esse conteúdo. Se um arquivo `docs/adr/` faz você documentar trade-offs de forma organizada, ele serve à avaliação. Se vira burocracia com 10 ADRs de decisões triviais, a Red Hat diria que é overkill.

**Veredito:** não há consenso de que ADR "impressione" recrutador. Há evidência de que **documentar trade-offs** é valorizado — e ADR é *um* formato para isso. Trate como **opinião informada, não como fato**.

---

## 5. Honestidade sobre limitações

**Também não encontrei pesquisa/declaração direta de tech leads sobre "README que admite não estar em produção".** O que encontrei aponta **consistentemente na direção de que é sinal de maturidade**, mas é evidência secundária.

- **SOLTECH, out/2026 [SECUNDÁRIA]:** nas perguntas de avaliação, inclui explicitamente *"known limitations"* (limitações conhecidas) como um dos *"known limitations, and evidence of iteration"* a serem revistos no portfólio. E: engenheiros "precisam **explicar limitações**". → https://soltech.net/what-do-hiring-managers-actually-look-for-in-a-github-portfolio/
- **Hyperskill, jun/2026 [SECUNDÁRIA]:** recomenda uma seção de *"o que o desenvolvedor faria diferente numa próxima iteração"* e diz que *"essa última seção — autocrítica — é particularmente valiosa porque demonstra que você avalia seu próprio trabalho criticamente, que é uma habilidade profissional central."* Também marca **"projetos ambiciosos 60% completos ... sinalizam falta de follow-through"** — ou seja, o problema não é admitir limitação, é **não terminar o escopo prometido**. → https://hyperskill.org/blog/post/building-a-developer-portfolio-in-2026-what-actually-gets-attention
- **HN "It's OK for your open source library to be a bit shitty", 2015 [OPINIÃO, DATADA]:** "Afasta as pessoas se não estiver pronto para produção ... **às vezes subvenda e nunca sobrevenda**. Remova qualquer razão para as expectativas das pessoas estarem fora de controle." → https://news.ycombinator.com/item?id=23208779 — o princípio (não superestimar) envelheceu bem; a discussão é de 2015.

**Distinção importante que a evidência sugere:** existe diferença entre
- **"não está em produção, aqui estão as limitações e o que eu faria diferente"** → lido como **maturidade**; e
- **"work in progress, 60% pronto, autenticação stub, sem testes"** → lido como **falta de acabamento**.

A linha entre os dois é se o **escopo declarado** está completo. Uma API de aluguel de carros com escopo "CRUD + regras de negócio + testes" completa, com uma seção "Limitações / próximos passos", tende ao primeiro caso.

**Veredito:** é sinal de maturidade, com **base secundária consistente, não pesquisa primária**. Ninguém argumentou o contrário nas fontes que encontrei — mas isso pode ser viés de seleção (só achei fontes que concordam).

---

## 6. O que NÃO fazer (antipadrões)

Lista consolidada; cada item com fonte. Nem tudo é consenso — marco onde é opinião.

| Antipadrão | Fonte | Tipo |
|---|---|---|
| **Badge spam** (12 badges: "made with love", "code style: prettier", "contributions welcome") | Ofer Shapira, DEV, mar/2026: "**Fique com 3–5**: versão, CI, licença... todo badge além disso é ruído." | **[OPINIÃO]** de mantenedor experiente; repetido por daily.dev (2024) | 
| | https://dev.to/ofershap/8-readme-mistakes-killing-your-github-stars-and-how-to-fix-them-ong · https://daily.dev/blog/readme-badges-github-types-and-uses/ | |
| **Descrição genérica em vez de gancho** ("MyLib é uma biblioteca utilitária para JavaScript") | Shapira, 2026: "Ninguém se importa... me dê um motivo para continuar lendo na primeira linha." | **[OPINIÃO]** |
| **Installation > 30 s / não roda do clone limpo** | Shapira, 2026: "do README ao código rodando em < 30 s". Hyperskill, 2026: "roda de um clone limpo". | **[OPINIÃO] + [SECUNDÁRIA]** |
| **README de 500 linhas / precisa de sumário** | Shapira, 2026: "O README é uma landing page, não um manual." | **[OPINIÃO]** (com contraponto: makeareadme diz "longo demais > curto demais") |
| **Exemplos desatualizados** (API mudou, README mostra a antiga) | Shapira, 2026: "silent killer ... a pessoa copia, quebra, fecha a aba e não volta." | **[OPINIÃO]** |
| **Lista de features inflada / claims não suportados por código** | romainlespinasse.dev, abr/2026: skill que "checa sistematicamente que cada claim de feature na documentação é suportada pela implementação real". GitSkins, jul/2026: "unsupported claims". | **[SECUNDÁRIA]** — https://www.romainlespinasse.dev/posts/verify-readme-features-skill/ · https://www.gitskins.com/blog/github-profile-readme-mistakes |
| **Prints/imagens irrelevantes & "huge animations"** | GitSkins, jul/2026: "huge animations, unclear project hierarchy". | **[SECUNDÁRIA]** |
| **"Tecnologias: HTML, CSS, JavaScript" (lista de tecnologias como preenchimento)** | Parcialmente inferido. As fontes falam de *"weak intros"* e *"badge spam"* mas **não achei uma fonte que critique nominalmente "Tecnologias: HTML, CSS, JS"**. Trate como plausível, **não verificado**. | **[NÃO VERIFICADO]** |
| **GitHub vazio/bagunçado é pior que nenhum GitHub** | applyfinch, 2026 | **[SECUNDÁRIA]** |
| **Repos só de tutorial / clones de tutorial** | Hyperskill, 2026: "sinalizam que você completou tutoriais, não que sabe construir software." | **[SECUNDÁRIA]** |
| **Commit history "Initial commit" + "Finished project"** | Hyperskill, 2026; applyfinch, 2026 ("Recruiters notice dates"). | **[SECUNDÁRIA]** |

**Sobre "tutorial clone":** o critério mais repetido em 2026 para detectar isso é a assinatura do README — "descreve o que o projeto faz em vez de por que decisões foram tomadas". Isso conecta diretamente com a diretriz da Hyperskill de incluir arquitetura e trade-offs.

---

## Recomendações acionáveis

Ordenadas por (impacto na avaliação × baixo esforço), e ancoradas nas fontes acima.

### README (o artefato mais lido na janela curta)
1. **Primeira linha = gancho com valor, não descrição genérica.** ("API que modela reserva de veículos, disponibilidade e preços — Spring Boot/Java 25/PostgreSQL.") Fonte: Shapira 2026; applyfinch 2026.
2. **Estrutura sugerida (sua intenção está alinhada com o que as fontes 2026 descrevem):**
   1. Gancho de uma frase.
   2. **O problema que resolve e para quem** (domínio de aluguel de carros).
   3. Como resolve / modelo de domínio em 3–5 linhas + trade-offs-chave.
   4. **Como rodar do clone limpo** (comandos testados, requisitos, variáveis de ambiente).
   5. Decisões arquiteturais e por quê (aqui é onde a parte técnica "pesada" entra — depois da primeira metade).
   6. Testes (o que cobrem, incluindo casos de falha).
   7. **Limitações / o que eu faria diferente.**
   8. Detalhes profundos → mover para `/docs`, não apagar.
   Fontes: Hyperskill 2026; Superdev; makeareadme; Shapira 2026 (README como landing page).
3. **Mire ~200–400 palavras na parte principal**, movendo profundidade para `/docs`. Contraponto a considerar: makeareadme diz que "longo demais > curto demais" — então *mova*, não *delete*. Fonte: Shapira 2026; makeareadme.
4. **Badges: 3–5, no máximo** (build/CI, versão de Java, licença). Remova qualquer badge decorativo. Fonte: Shapira 2026.
5. **Sem prints irrelevantes.** Se não houver UI, prefira exemplos de requisição/resposta (`curl` + JSON) ou um diagrama de domínio. Fonte: GitSkins 2026; Shapira 2026.

### Comentários / legibilidade (seu foco declarado)
6. **Adote o teste "se eu remover, perco informação?"** Comentário que só traduz sintaxe → remover. Base empírica: Abdelsalam et al. 2026 (efeito de comentários varia de −30% a +34%). 
7. **Comente decisões e trade-offs**, especialmente onde o Spring/Java 25 não deixa óbvio (ex.: por que `@Transactional` aqui, por que essa estratégia de concorrência ao reservar um carro). Fontes: SOLTECH 2026; Hyperskill 2026.
8. **Javadoc: use com critério e consistência, não em todo método.** Em Java 23+ prefira Markdown (`///`) e não misture com tags legadas. Fontes: Sonar 2025; InfoWorld; Oracle.
9. **Nunca deixe comentário obsoleto** — pior que não ter. Fonte: KIT (didático); InfoWorld.

### ADRs
10. **Use ADR, se usar, como o lugar do "por que escolhi X"** — o conteúdo que o entrevistador vai querer. Mantenha 3–6 ADRs curtos sobre decisões *reais e significativas* (ex.: estratégia de disponibilidade de veículos, modelo de preços). **Evite ADR para decisões triviais** (Red Hat: overkill em projeto pequeno). Marque isto como prática defensável, não como "impressiona recrutador" — essa ligação **não é verificável**.

### Honestidade
11. **Inclua uma seção "Limitações / o que eu faria diferente".** A evidência (secundária, consistente) indica maturidade — desde que o **escopo declarado esteja completo**. Não anuncie como "60% pronto / WIP". Fontes: SOLTECH 2026; Hyperskill 2026; HN 2015 (subvenda, não sobrevenda).
12. **Deixe claro que é projeto de portfólio, não serviço em produção.** Isso é o que as fontes recomendam; não encontrei nenhuma voz defendendo o oposto.

### Antipadrões a eliminar
13. Sem badge spam; sem lista de features inflada (cada claim deve ter código que sustenta); sem prints decorativos; sem README que precisa de sumário; sem exemplos desatualizados (rode os exemplos após mudanças); commit history incremental com mensagens que expliquem intenção.

---

## Lacunas honestas (o que eu NÃO consegui verificar)

- **Proporção README vs. código por eye-tracking em triagem de candidatos:** não existe estudo público que eu tenha encontrado. Os números de tempo (6 s / 30 s / 90 s) **se contradizem** e vêm de blogs com interesse. Não os apresente como fato.
- **Texto completo de posts de LinkedIn (Nitin Aggarwal, David Chung):** bloqueado; usei apenas os trechos dos resultados de busca. Marcados como opinião individual.
- **PDF completo do estudo de comentários (Springer, Abdelsalam et al. 2026):** bloqueado; os números −30%/+34% vêm do abstract e de resumos de indexação. Corroborado por 4 indexadores, mas não lido na íntegra.
- **Reddit threads** (`r/cscareerquestions` "Always write a clear README", e o thread de Spring Boot de referência): **não renderizaram** para mim (erro/vazio). Sei que existem (o thread "Always write a clear README" tem ~1,8 mil upvotes, ~183 comentários, conforme apareceu em buscas relacionadas) mas **não li o conteúdo** — trate como pista, não como fonte.
- **"Tecnologias: HTML, CSS, JavaScript" como antipadrão nomeado:** **não achei fonte** que critique isso nominalmente. É plausível e coerente com "weak intros"/"badge spam", mas não é verificado.
- **Opinião de tech leads sobre ADRs/honestidade em portfólio especificamente:** só achei conteúdo de empresas de recrutamento/educação, não de tech leads identificáveis. Pode haver viés de seleção — todas as fontes concordam entre si, o que não prova consenso.

---

## Fontes (URLs)

**Sobre avaliação do repositório no processo seletivo**
- https://applyfinch.com/blog/github-on-resume (mar/2026, atualizado out/2026)
- https://soltech.net/what-do-hiring-managers-actually-look-for-in-a-github-portfolio/ (out/2026)
- https://hyperskill.org/blog/post/building-a-developer-portfolio-in-2026-what-actually-gets-attention (jun/2026)
- https://onehour.digital/blog/time-spent-reviewing-portfolio-websites-statistics (2026)
- https://priygop.com/blog/how-to-build-a-github-portfolio-that-gets-you-hired (2026)
- https://flatironschool.com/blog/github-profile-and-git-practices-for-job-seekers
- https://www.linkedin.com/posts/nitinagam_i-check-every-candidates-github-profile-activity-7505815292065931265-339H
- https://www.linkedin.com/posts/davidkchung_github-is-a-gold-mine-for-technical-recruiters-activity-7473754020046798848-Dy3S
- https://github.com/orgs/community/discussions/169760

**Estrutura de README**
- https://www.makeareadme.com/
- https://github.com/banesullivan/README
- https://spin.atomicobject.com/valuable-readme/
- https://www.freecodecamp.org/news/how-to-write-a-good-readme-file/
- https://utrechtuniversity.github.io/workshop-computational-reproducibility/chapters/readme-files.html
- https://www.superdevacademy.com/en/blogs/portfolio-programmer-junior-first-job-ai-era
- https://www.sefism.com/questions/how-to-build-a-developer-portfolio-as-a-beginner/
- https://www.linkedin.com/top-content/career/building-a-personal-portfolio/how-to-build-a-developer-portfolio-as-a-junior/

**Comentários / legibilidade (pesquisa)**
- https://link.springer.com/article/10.1007/s10664-025-10721-2 (Abdelsalam et al., 2026)
- https://dl.acm.org/doi/10.1007/s10664-025-10721-2 (mesmo estudo, ACM)
- https://arxiv.org/html/2602.21833v1 (Peitek et al., TOSEM, fev/2026)
- https://www.se.cs.uni-saarland.de/publications/docs/APA+26.pdf (Alznauer et al., 2026)
- https://arxiv.org/html/2605.23008v1 (reliability of code comprehension proxies)

**Comentários / Javadoc (prática)**
- https://www.sonarsource.com/blog/java-23-embrace-the-new-era-of-code-comments/ (jul/2025)
- https://www.infoworld.com/article/2162395/more-effective-javadoc.html
- https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html
- https://indico.kit.edu/event/5258/sessions/4921/attachments/9676/16392/DocumentationBasics.pdf
- https://news.ycombinator.com/item?id=49410932 (ago/2026)
- https://www.reddit.com/r/java/comments/ncs7io/do_we_need_comments_in_our_code/
- https://www.reddit.com/r/ProgrammerHumor/comments/1wgllyu/

**ADRs**
- https://www.redhat.com/en/blog/architecture-decision-records
- https://engineering.atspotify.com/2020/04/when-should-i-write-an-architecture-decision-record (abr/2020 — DATADA)
- https://niels.nu/blog/2018/lightweight-architecture-decision-records/ (2018 — DATADA)
- https://testdouble.com/insights/architecture-decision-records-help-you-your-team-and-future-teams
- https://learn.microsoft.com/en-us/azure/well-architected/architect-role/architecture-decision-record

**Honestidade / limitações**
- https://news.ycombinator.com/item?id=23208779 (2015 — DATADA)
- (ver também SOLTECH 2026 e Hyperskill 2026 acima)

**Antipadrões**
- https://dev.to/ofershap/8-readme-mistakes-killing-your-github-stars-and-how-to-fix-them-ong (mar/2026)
- https://daily.dev/blog/readme-badges-github-types-and-uses/ (mar/2024)
- https://www.gitskins.com/blog/github-profile-readme-mistakes (jul/2026)
- https://www.romainlespinasse.dev/posts/verify-readme-features-skill/ (abr/2026)
