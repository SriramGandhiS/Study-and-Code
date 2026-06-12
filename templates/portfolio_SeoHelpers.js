/**
 * SeoHelpers.js — SEO utility functions for portfolio meta management.
 *
 * Functions:
 *   buildMetaTags(pageConfig)         — Generates <meta> tag descriptor objects
 *   generateJsonLd(type, data)        — Builds JSON-LD structured data strings
 *   updateDocumentTitle(title)        — Updates document.title with site suffix
 *   buildCanonicalUrl(path)           — Constructs absolute canonical URL
 *   generateOpenGraphTags(config)     — Generates Open Graph meta descriptors
 *
 * Add:    feat: add SEO helper utilities for meta tags and JSON-LD generation
 * Delete: refactor: replace SEO helpers with react-helmet-async
 */

// ─── Site-wide SEO defaults ──────────────────────────────────────────────────

export const SEO_DEFAULTS = Object.freeze({
  author:      'Sriram Venkatesh',
  siteName:    'Sriram Venkatesh — Portfolio',
  baseUrl:     'https://sriramvenkatesh.dev',
  description: 'Full-stack developer specializing in Spring Boot, React, and AI-powered web applications. ' +
               'Building scalable, production-ready software with clean architecture.',
  keywords: [
    'Sriram Venkatesh',
    'Full Stack Developer',
    'Spring Boot Developer',
    'React Developer',
    'Java Developer',
    'AI Application Developer',
    'Portfolio',
    'Microservices',
    'REST API',
    'Software Engineer India',
  ],
  twitterHandle: '@sriramvenkat_dev',
  locale:        'en_IN',
  themeColor:    '#6366f1',
  ogImageUrl:    'https://sriramvenkatesh.dev/og-image.png',
  ogImageAlt:    'Sriram Venkatesh — Full Stack Developer',
});

// ─── buildMetaTags ───────────────────────────────────────────────────────────

/**
 * Builds an array of meta tag descriptor objects from a page configuration.
 * Each object has { name?, property?, content } representing one <meta> tag.
 *
 * @param {Object} pageConfig - page-specific overrides
 * @param {string} [pageConfig.title]       - page title (without site suffix)
 * @param {string} [pageConfig.description] - page description
 * @param {string} [pageConfig.keywords]    - comma-separated keywords or array
 * @param {string} [pageConfig.path]        - page path (e.g. '/projects')
 * @param {string} [pageConfig.ogImage]     - Open Graph image URL override
 * @returns {Array<{name?: string, property?: string, content: string}>}
 */
export function buildMetaTags(pageConfig = {}) {
  const desc    = pageConfig.description || SEO_DEFAULTS.description;
  const kwArray = Array.isArray(pageConfig.keywords)
    ? pageConfig.keywords
    : typeof pageConfig.keywords === 'string'
      ? pageConfig.keywords.split(',').map((k) => k.trim())
      : SEO_DEFAULTS.keywords;
  const keywords = kwArray.join(', ');
  const canonical = buildCanonicalUrl(pageConfig.path || '/');

  return [
    { name: 'description',        content: desc },
    { name: 'keywords',           content: keywords },
    { name: 'author',             content: SEO_DEFAULTS.author },
    { name: 'robots',             content: 'index, follow' },
    { name: 'theme-color',        content: SEO_DEFAULTS.themeColor },
    { name: 'viewport',           content: 'width=device-width, initial-scale=1.0' },
    { name: 'language',           content: 'English' },
    { property: 'og:type',        content: 'website' },
    { property: 'og:url',         content: canonical },
    { property: 'og:site_name',   content: SEO_DEFAULTS.siteName },
    { property: 'og:locale',      content: SEO_DEFAULTS.locale },
    ...generateOpenGraphTags({
      title:       pageConfig.title ? `${pageConfig.title} | ${SEO_DEFAULTS.author}` : SEO_DEFAULTS.siteName,
      description: desc,
      image:       pageConfig.ogImage || SEO_DEFAULTS.ogImageUrl,
      imageAlt:    SEO_DEFAULTS.ogImageAlt,
      url:         canonical,
    }),
    { name: 'twitter:card',        content: 'summary_large_image' },
    { name: 'twitter:site',        content: SEO_DEFAULTS.twitterHandle },
    { name: 'twitter:creator',     content: SEO_DEFAULTS.twitterHandle },
    { name: 'twitter:title',       content: pageConfig.title || SEO_DEFAULTS.siteName },
    { name: 'twitter:description', content: desc },
    { name: 'twitter:image',       content: pageConfig.ogImage || SEO_DEFAULTS.ogImageUrl },
  ];
}

// ─── generateJsonLd ──────────────────────────────────────────────────────────

/**
 * Generates a JSON-LD structured data string for use in a <script type="application/ld+json"> tag.
 *
 * Supported types: 'Person', 'WebSite', 'BreadcrumbList', 'SoftwareApplication'
 *
 * @param {'Person' | 'WebSite' | 'BreadcrumbList' | 'SoftwareApplication'} type
 * @param {Object} data - type-specific data fields
 * @returns {string} minified JSON-LD string
 */
export function generateJsonLd(type, data = {}) {
  const base = { '@context': 'https://schema.org', '@type': type };

  const schemas = {
    Person: () => ({
      ...base,
      name:        data.name        || SEO_DEFAULTS.author,
      url:         data.url         || SEO_DEFAULTS.baseUrl,
      jobTitle:    data.jobTitle    || 'Full Stack Software Engineer',
      description: data.description || SEO_DEFAULTS.description,
      sameAs:      data.sameAs      || [
        'https://github.com/sriramvenkatesh',
        'https://linkedin.com/in/sriramvenkatesh',
        'https://twitter.com/sriramvenkat_dev',
      ],
      knowsAbout: data.knowsAbout || ['Java', 'Spring Boot', 'React', 'Node.js', 'AI/ML Integration'],
    }),

    WebSite: () => ({
      ...base,
      name:              data.name    || SEO_DEFAULTS.siteName,
      url:               data.url     || SEO_DEFAULTS.baseUrl,
      description:       data.description || SEO_DEFAULTS.description,
      author:            { '@type': 'Person', name: SEO_DEFAULTS.author },
      potentialAction: {
        '@type':       'SearchAction',
        target:        `${SEO_DEFAULTS.baseUrl}/projects?q={search_term_string}`,
        'query-input': 'required name=search_term_string',
      },
    }),

    BreadcrumbList: () => ({
      ...base,
      itemListElement: (data.items || []).map((item, idx) => ({
        '@type':    'ListItem',
        position:   idx + 1,
        name:       item.name,
        item:       buildCanonicalUrl(item.path),
      })),
    }),

    SoftwareApplication: () => ({
      ...base,
      name:            data.name        || 'Portfolio Project',
      description:     data.description || '',
      applicationCategory: data.category || 'WebApplication',
      url:             data.url || SEO_DEFAULTS.baseUrl,
      author:          { '@type': 'Person', name: SEO_DEFAULTS.author },
      programmingLanguage: data.languages || ['Java', 'JavaScript', 'TypeScript'],
    }),
  };

  const builder = schemas[type];
  if (!builder) {
    console.warn(`[generateJsonLd] Unsupported schema type: "${type}"`);
    return JSON.stringify(base);
  }
  return JSON.stringify(builder());
}

// ─── updateDocumentTitle ─────────────────────────────────────────────────────

/**
 * Updates the browser document title with a consistent site suffix.
 *
 * @param {string} title - page-specific title. Pass null/empty for home page.
 * @returns {string} the full title that was set
 */
export function updateDocumentTitle(title) {
  const fullTitle = title
    ? `${title} | ${SEO_DEFAULTS.author}`
    : SEO_DEFAULTS.siteName;
  if (typeof document !== 'undefined') {
    document.title = fullTitle;
  }
  return fullTitle;
}

// ─── buildCanonicalUrl ───────────────────────────────────────────────────────

/**
 * Constructs a fully-qualified canonical URL from a relative path.
 * Normalizes trailing slashes and encodes special characters.
 *
 * @param {string} path - relative path (e.g., '/projects' or 'about')
 * @returns {string} absolute canonical URL
 */
export function buildCanonicalUrl(path = '/') {
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  // Normalize double slashes
  const normalized = cleanPath.replace(/\/+/g, '/');
  // Remove trailing slash unless it's root
  const final = normalized !== '/' && normalized.endsWith('/')
    ? normalized.slice(0, -1)
    : normalized;
  return `${SEO_DEFAULTS.baseUrl}${final}`;
}

// ─── generateOpenGraphTags ───────────────────────────────────────────────────

/**
 * Generates Open Graph meta tag descriptor objects.
 *
 * @param {Object} config
 * @param {string} config.title       - OG title
 * @param {string} config.description - OG description
 * @param {string} config.image       - OG image URL
 * @param {string} config.imageAlt    - OG image alt text
 * @param {string} config.url         - Canonical URL
 * @returns {Array<{property: string, content: string}>}
 */
export function generateOpenGraphTags(config = {}) {
  const tags = [
    { property: 'og:title',       content: config.title       || SEO_DEFAULTS.siteName },
    { property: 'og:description', content: config.description || SEO_DEFAULTS.description },
    { property: 'og:image',       content: config.image       || SEO_DEFAULTS.ogImageUrl },
    { property: 'og:image:alt',   content: config.imageAlt    || SEO_DEFAULTS.ogImageAlt },
    { property: 'og:image:width', content: '1200' },
    { property: 'og:image:height',content: '630' },
  ];
  if (config.url) tags.push({ property: 'og:url', content: config.url });
  return tags;
}

// ─── Default export: grouped utilities ───────────────────────────────────────

export default {
  SEO_DEFAULTS,
  buildMetaTags,
  generateJsonLd,
  updateDocumentTitle,
  buildCanonicalUrl,
  generateOpenGraphTags,
};
