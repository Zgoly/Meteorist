"use strict";

const $ = (sel, ctx = document) => ctx.querySelector(sel);
const $$ = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

function unescapeDoubleQuotes(str) {
  if (typeof str !== "string") return str;
  return str.split('\\"').join('"');
}

function humanize(name) {
  return String(name)
    .replace(/-/g, " ")
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

function formatFileSize(bytes) {
  if (!bytes) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), sizes.length - 1);
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
}

function getTimeAgo(dateString) {
  const date = new Date(dateString);
  if (isNaN(date)) return "";
  const seconds = Math.floor((Date.now() - date) / 1000);
  const intervals = [
    { label: "year", seconds: 31536000 },
    { label: "month", seconds: 2592000 },
    { label: "week", seconds: 604800 },
    { label: "day", seconds: 86400 },
    { label: "hour", seconds: 3600 },
    { label: "minute", seconds: 60 },
  ];
  for (const interval of intervals) {
    const count = Math.floor(seconds / interval.seconds);
    if (count > 0) return `updated ${count} ${interval.label}${count !== 1 ? "s" : ""} ago`;
  }
  return "updated just now";
}

function formatDate(dateString) {
  const date = new Date(dateString);
  if (isNaN(date)) return "";
  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text != null) node.textContent = text;
  return node;
}

const toastNode = $("#toast");
const toastText = $("#toast-text");
let toastTimer = null;

function showToast(message) {
  if (!toastNode) return;
  toastText.textContent = message;
  const host = document.querySelector("dialog[open], [popover]:popover-open") || document.body;
  if (toastNode.parentElement !== host) host.appendChild(toastNode);
  void toastNode.offsetWidth;
  toastNode.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => {
    toastNode.classList.remove("show");
    window.setTimeout(() => {
      if (toastNode.parentElement !== document.body) document.body.appendChild(toastNode);
    }, 400);
  }, 1800);
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    // Fallback for older browsers / insecure contexts
    const ta = document.createElement("textarea");
    ta.value = text;
    ta.style.cssText = "position:fixed;opacity:0";
    document.body.appendChild(ta);
    ta.select();
    let ok = false;
    try {
      ok = document.execCommand("copy");
    } catch {}
    ta.remove();
    return ok;
  }
}

const menuToggle = $("#menu-toggle");

function setMenu(open) {
  document.body.classList.toggle("nav-open", open);
  menuToggle?.setAttribute("aria-expanded", String(open));
}

menuToggle?.addEventListener("click", () => setMenu(!document.body.classList.contains("nav-open")));
$$("#nav-links a").forEach((a) => a.addEventListener("click", () => setMenu(false)));
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") setMenu(false);
});

const progressBar = $("#scroll-progress");
const toTopBtn = $("#to-top");
const navAnchors = $$('#nav-links a[href^="#"]');
const modulesToolbar = $("#modules-toolbar");
const toolbarStickyTop = modulesToolbar ? parseFloat(getComputedStyle(modulesToolbar).top) || 0 : 0;
let scrollQueued = false;

function onScrollFrame() {
  scrollQueued = false;
  const y = window.scrollY;
  const max = document.documentElement.scrollHeight - window.innerHeight;

  if (progressBar) {
    progressBar.style.transform = `scaleX(${max > 0 ? Math.min(y / max, 1) : 0})`;
  }
  toTopBtn?.classList.toggle("visible", y > 700);

  // Give the module toolbar a shadow while it is pinned below the header
  if (modulesToolbar) {
    modulesToolbar.classList.toggle(
      "is-stuck",
      modulesToolbar.getBoundingClientRect().top <= toolbarStickyTop + 1,
    );
  }

  // Active nav link: last section above the header line
  let activeId = null;
  for (const a of navAnchors) {
    const section = document.querySelector(a.getAttribute("href"));
    if (section && section.getBoundingClientRect().top <= 120) {
      activeId = a.getAttribute("href");
    }
  }
  for (const a of navAnchors) {
    a.classList.toggle("active", a.getAttribute("href") === activeId);
  }
}

window.addEventListener(
  "scroll",
  () => {
    if (!scrollQueued) {
      scrollQueued = true;
      requestAnimationFrame(onScrollFrame);
    }
  },
  { passive: true },
);

toTopBtn?.addEventListener("click", () => {
  window.scrollTo({
    top: 0,
    behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth",
  });
});

onScrollFrame();

const revealObserver = new IntersectionObserver(
  (entries) => {
    for (const entry of entries) {
      if (entry.isIntersecting) {
        entry.target.classList.add("in");
        revealObserver.unobserve(entry.target);
      }
    }
  },
  { rootMargin: "0px 0px -8% 0px", threshold: 0.01 },
);

$$(".reveal").forEach((node) => revealObserver.observe(node));

// Clicking an in-page anchor inside the popover should close it.
$$('#install-popover a[href^="#"]').forEach((a) => {
  a.addEventListener("click", () => {
    $("#install-popover")?.hidePopover?.();
    setMenu(false);
  });
});

let releaseData = null;

function setMcVersion(tag) {
  // Tag scheme: v<mcVersion>-<revision>, e.g. v26.1.2-1 or v1.21.11-2
  const m = /^v?(\d+\.\d+\.\d+)-\d+$/.exec(tag || "");
  if (!m) return;
  $$(".js-mc-version").forEach((node) => {
    node.textContent = `Minecraft ${m[1]}`;
    node.hidden = false;
  });
  $$(".js-mc-dot").forEach((node) => {
    node.hidden = false;
  });
  $$(".js-mc-version-inline").forEach((node) => {
    node.textContent = `Minecraft ${m[1]}`;
  });
}

async function setupReleaseInfo() {
  try {
    const res = await fetch("generated/release-info.json");
    releaseData = await res.json();
    if (!releaseData.url || !releaseData.tag_name) return;

    $("#download-link").href = releaseData.url;
    $$(".tag-name").forEach((node) => {
      node.textContent = releaseData.tag_name;
    });
    $$(".file-size").forEach((node) => {
      node.textContent = formatFileSize(releaseData.size);
    });
    $$(".time-ago").forEach((node) => {
      node.textContent = getTimeAgo(releaseData.updated_at);
      node.title = formatDate(releaseData.updated_at);
    });
    setMcVersion(releaseData.tag_name);

    // Keep the SEO schema fresh too
    const schemaNode = $("#schema-jsonld");
    if (schemaNode) {
      try {
        const schema = JSON.parse(schemaNode.textContent);
        const software = Array.isArray(schema)
          ? schema.find((entry) => entry["@type"] === "SoftwareApplication")
          : schema;
        if (software) {
          software.softwareVersion = releaseData.tag_name;
          software.downloadUrl = releaseData.url;
          schemaNode.textContent = JSON.stringify(schema, null, 8);
        }
      } catch (error) {
        console.warn("Failed to update schema.org data:", error);
      }
    }

    // Changelog section
    const changelogBody = $("#changelog-body");
    if (changelogBody) {
      changelogBody.replaceChildren();
      if (releaseData.body) {
        const frag = document.createDocumentFragment();
        const lines = String(releaseData.body).split(/\r?\n/);
        let inList = false;
        let ul = null;

        const closeList = () => {
          if (inList && ul) {
            frag.appendChild(ul);
            ul = null;
            inList = false;
          }
        };

        const renderInline = (text) => {
          const span = document.createElement("span");
          span.textContent = text;
          let html = span.innerHTML;
          html = html.replace(
            /(\[([^\]]+)\]\((https?:[^)\s]+)\))|(`[^`]+`)|(\*\*[^*]+\*\*)|(https?:\/\/[^\s<]+)/g,
            (match, mdLink, linkText, linkUrl, code, bold, bareUrl) => {
              if (mdLink) return '<a href="' + linkUrl + '" rel="noopener" target="_blank">' + linkText + "</a>";
              if (code) return "<code>" + code.slice(1, -1) + "</code>";
              if (bold) return "<strong>" + bold.slice(2, -2) + "</strong>";
              if (bareUrl) return '<a href="' + bareUrl + '" rel="noopener" target="_blank">' + bareUrl + "</a>";
              return match;
            },
          );
          return html;
        };

        for (const rawLine of lines) {
          const trimmed = rawLine.trim();
          if (!trimmed) continue;
          const heading = /^#{1,6}\s+(.*)$/.exec(trimmed);
          const bullet = /^[-*]\s+(.*)$/.exec(trimmed);
          if (heading) {
            closeList();
            const h = document.createElement("h4");
            h.innerHTML = renderInline(heading[1]);
            frag.appendChild(h);
          } else if (bullet) {
            if (!inList) {
              ul = document.createElement("ul");
              inList = true;
            }
            const li = document.createElement("li");
            li.innerHTML = renderInline(bullet[1]);
            ul.appendChild(li);
          } else {
            closeList();
            const p = document.createElement("p");
            p.innerHTML = renderInline(trimmed);
            frag.appendChild(p);
          }
        }
        closeList();
        changelogBody.appendChild(frag);
      } else {
        const p = document.createElement("p");
        p.className = "dim";
        p.textContent = "No release notes for this version.";
        changelogBody.appendChild(p);
      }
    }
  } catch (error) {
    console.error("Failed to load release info:", error);
    const changelogBody = $("#changelog-body");
    if (changelogBody) {
      changelogBody.replaceChildren();
      const p = document.createElement("p");
      p.className = "dim";
      p.innerHTML =
        'Couldn\'t load release notes. See <a href="https://github.com/Zgoly/Meteorist/releases" rel="noopener" target="_blank">GitHub Releases</a>.';
      changelogBody.appendChild(p);
    }
  }
  keepAnchorInView();
}

// Every button marked as a download trigger starts the download
// (the popover itself opens natively via popovertarget)
document.addEventListener("click", (e) => {
  const trigger = e.target.closest("[data-download-trigger]");
  if (!trigger || !releaseData?.url) return;
  setMenu(false);
  const link = document.createElement("a");
  link.href = releaseData.url;
  link.rel = "noopener";
  document.body.appendChild(link);
  link.click();
  link.remove();
});

setupReleaseInfo();

async function loadGitHubStats() {
  const parser = new DOMParser();

  async function fetchBadgeText(url) {
    const res = await fetch(url);
    const doc = parser.parseFromString(await res.text(), "image/svg+xml");
    const text = doc.querySelector("text")?.textContent?.trim();
    return text && text !== "invalid" ? text : null;
  }

  const [stars, downloads] = await Promise.all([
    fetchBadgeText("https://img.shields.io/github/stars/Zgoly/Meteorist?style=flat-square&label=").catch(() => null),
    fetchBadgeText("https://img.shields.io/github/downloads/Zgoly/Meteorist/total?style=flat-square&label=").catch(
      () => null,
    ),
  ]);

  if (stars) {
    $$(".js-star-count").forEach((node) => {
      node.textContent = stars;
    });
  } else {
    $("#github-star-badge")?.remove();
    $$(".js-star-box").forEach((node) => node.remove());
  }

  if (downloads) {
    $$(".js-dl-count").forEach((node) => {
      node.textContent = downloads;
    });
  } else {
    $$(".js-dl-box").forEach((node) => node.remove());
  }

  keepAnchorInView();
}

loadGitHubStats();

const SETTING_TYPE_LABELS = {
  BoolSetting: "toggle",
  IntSetting: "number",
  DoubleSetting: "number",
  StringSetting: "text",
  StringListSetting: "text list",
  ColorSetting: "color",
  EnumSetting: "enum",
  BlockListSetting: "block list",
  ItemListSetting: "item list",
  EntityTypeListSetting: "entity list",
  SoundEventListSetting: "sound list",
  BlockPosSetting: "position",
  Vector3dSetting: "vector",
  KeybindSetting: "keybind",
  ModuleListSetting: "module list",
};

function stripCodeMarkers(value) {
  return String(value).trim().replace(/^`|`$/g, "").replace(/\\"/g, '"');
}

function renderDefaultValue(type, value) {
  const wrap = el("span", "setting-meta-value");

  if (Array.isArray(value)) {
    const list = el("span", "default-list");
    value.slice(0, 6).forEach((item) => list.appendChild(el("code", "setting-default", simplifyValue(item))));
    if (value.length > 6) list.appendChild(el("code", "setting-default", `+${value.length - 6} more`));
    wrap.appendChild(list);
    return wrap;
  }

  const raw = stripCodeMarkers(value ?? "");

  if (type === "ColorSetting") {
    const m = /SettingColor\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*(\d+)\s*)?\)/.exec(raw);
    if (m) {
      const [r, g, b, a] = [+m[1], +m[2], +m[3], m[4] != null ? +m[4] : 255];
      const swatch = el("span", "color-swatch");
      swatch.style.background = `rgba(${r},${g},${b},${(a / 255).toFixed(2)})`;
      wrap.appendChild(swatch);
      wrap.appendChild(el("code", "setting-default", `rgba(${r}, ${g}, ${b}, ${a})`));
      return wrap;
    }
  }

  if (type === "BoolSetting") {
    const on = /^(true|`?true`?)$/i.test(raw);
    wrap.appendChild(el("span", `setting-default ${on ? "bool-on" : "bool-off"}`, on ? "on" : "off"));
    return wrap;
  }

  const listOf = /^List\.of\((.*)\)$/s.exec(raw);
  if (listOf) {
    const items = listOf[1]
      .split(",")
      .map((s) => s.trim().replace(/^"|"$/g, ""))
      .filter(Boolean);
    const list = el("span", "default-list");
    items.slice(0, 6).forEach((item) => list.appendChild(el("code", "setting-default", item)));
    if (items.length > 6) list.appendChild(el("code", "setting-default", `+${items.length - 6} more`));
    wrap.appendChild(list);
    return wrap;
  }

  wrap.appendChild(el("code", "setting-default", simplifyValue(raw)));
  return wrap;
}

function simplifyValue(raw) {
  let v = stripCodeMarkers(raw);
  const ctor = /^new\s+[\w.]+\((.*)\)$/s.exec(v);
  if (ctor) return `(${ctor[1].replace(/"/g, "")})`;
  if (/^[\w$]+\.[\w$]+$/.test(v) && !v.includes(" ")) return v.split(".").pop();
  return v;
}

function renderSetting(setting) {
  const props = setting.properties || {};
  const node = el("div", "setting");

  const row = el("div", "setting-row");
  row.appendChild(el("span", "setting-name", humanize(props.name || "-")));
  row.appendChild(el("span", "setting-type", SETTING_TYPE_LABELS[setting.type] || setting.type));
  node.appendChild(row);

  if (props.description) {
    node.appendChild(el("p", "setting-desc", unescapeDoubleQuotes(props.description)));
  }

  const meta = el("div", "setting-meta");
  let hasMeta = false;

  if (props.defaultValue != null) {
    const label = el("span", "label", "default");
    meta.appendChild(label);
    meta.appendChild(renderDefaultValue(setting.type, props.defaultValue));
    hasMeta = true;
  }

  const rangeParts = [];
  if (props.min != null && props.max != null) rangeParts.push(`${props.min} ... ${props.max}`);
  else if (Array.isArray(props.sliderRange)) rangeParts.push(`${props.sliderRange[0]} ... ${props.sliderRange[1]}`);
  else if (props.sliderMax != null) rangeParts.push(`<= ${props.sliderMax}`);
  else if (props.max != null) rangeParts.push(`<= ${props.max}`);
  else if (props.min != null) rangeParts.push(`>= ${props.min}`);

  if (rangeParts.length) {
    meta.appendChild(el("span", "setting-range", `range ${rangeParts.join(", ")}`));
    hasMeta = true;
  }

  if (hasMeta) node.appendChild(meta);
  return node;
}

function countSettings(module) {
  return Object.values(module.settings || {}).reduce((sum, list) => sum + (list?.length || 0), 0);
}

function moduleSearchIndex(module) {
  const parts = [
    module.name,
    humanize(module.name),
    module.name.replace(/-/g, ""),
    module.category,
    unescapeDoubleQuotes(module.description || ""),
  ];
  for (const [groupName, settings] of Object.entries(module.settings || {})) {
    parts.push(groupName);
    for (const s of settings || []) {
      parts.push(s.properties?.name || "", s.properties?.description || "");
    }
  }
  return parts.join(" ").toLowerCase();
}

const moduleBrowser = {
  cards: [],
  modulesBySlug: {},
  query: "",
  category: "all",
};

const moduleDialog = $("#module-dialog");

function openModuleDialog(module) {
  if (!moduleDialog) return;
  moduleDialog.textContent = "";

  const close = el("button", "dialog-close");
  close.type = "button";
  close.setAttribute("aria-label", "Close");
  close.appendChild(el("span", "material-symbols-rounded", "close"));
  close.addEventListener("click", () => moduleDialog.close());
  moduleDialog.appendChild(close);

  const head = el("div", "dialog-head");
  const icon = el("span", "material-symbols-rounded module-icon", "extension");
  icon.setAttribute("aria-hidden", "true");
  head.appendChild(icon);
  const title = el("h3", "dialog-title", humanize(module.name));
  title.id = "module-dialog-title";
  head.appendChild(title);
  moduleDialog.appendChild(head);

  const meta = el("div", "dialog-meta");
  if (module.category && module.category !== "Meteorist") {
    meta.appendChild(el("span", "module-cat", module.category));
  }
  const settingsCount = countSettings(module);
  const presetsCount = (module.presets || []).length;
  let metaCount = "";
  if (settingsCount) metaCount = `${settingsCount} setting${settingsCount !== 1 ? "s" : ""}`;
  else if (presetsCount) metaCount = `${presetsCount} HUD preset${presetsCount !== 1 ? "s" : ""}`;
  if (metaCount) meta.appendChild(el("span", "module-count", metaCount));
  moduleDialog.appendChild(meta);

  moduleDialog.appendChild(el("p", "dialog-desc", unescapeDoubleQuotes(module.description || "")));

  if (module.classPath) {
    const src = el("a", "dialog-source");
    const classPath = String(module.classPath).replace(/\\/g, "/");
    src.href = `https://github.com/Zgoly/Meteorist/blob/main/${classPath}#L${module.lineNumber || 1}`;
    src.target = "_blank";
    src.rel = "noopener";
    const srcIcon = el("span", "material-symbols-rounded small-icon", "code");
    srcIcon.setAttribute("aria-hidden", "true");
    src.appendChild(srcIcon);
    src.appendChild(el("span", "dialog-source-label", "View source on GitHub"));
    moduleDialog.appendChild(src);
  }

  const groups = module.settings || {};
  const groupNames = Object.keys(groups).filter((g) => groups[g]?.length);
  const presets = module.presets || [];

  if (groupNames.length || module.customGui) {
    const box = el("div", "dialog-settings");
    if (groupNames.length) {
      box.appendChild(el("p", "dialog-settings-title", "Settings"));
      for (const groupName of groupNames) {
        const group = el("div", "setting-group");
        if (groupName) group.appendChild(el("div", "setting-group-title", groupName));
        groups[groupName].forEach((setting) => group.appendChild(renderSetting(setting)));
        box.appendChild(group);
      }
    }
    if (module.customGui) {
      box.appendChild(el("p", "dialog-settings-title", "Custom GUI"));
      box.appendChild(
        el(
          "p",
          "settings-none",
          "This module uses a custom settings GUI, so some of its functionality cannot be shown here.",
        ),
      );
    }
    moduleDialog.appendChild(box);
  }

  if (presets.length) {
    const box = el("div", "dialog-settings");
    box.appendChild(el("p", "dialog-settings-title", "HUD presets"));
    for (const preset of presets) {
      const text = unescapeDoubleQuotes(preset.text);
      const titleNode = el("span", "hud-preset-title", preset.title);
      box.appendChild(
        makeCopyCard({ icon: "tv", titleNode, onCopy: text, extra: starscriptNode(text, "hud-preset-text") }),
      );
    }
    moduleDialog.appendChild(box);
  }

  if (!settingsCount && !presets.length && !module.customGui) {
    const box = el("div", "dialog-settings");
    box.appendChild(el("p", "dialog-settings-title", "Settings"));
    box.appendChild(el("p", "settings-none", "This module has no configurable settings."));
    moduleDialog.appendChild(box);
  }

  if (typeof moduleDialog.showModal === "function") moduleDialog.showModal();
  else moduleDialog.setAttribute("open", "");
}

// Click on the backdrop closes the dialog
moduleDialog?.addEventListener("click", (e) => {
  if (e.target === moduleDialog) moduleDialog.close();
});

function applyModuleFilters() {
  const q = moduleBrowser.query.trim().toLowerCase();
  let shown = 0;
  for (const card of moduleBrowser.cards) {
    const matchesCat = moduleBrowser.category === "all" || card.dataset.cat === moduleBrowser.category;
    const matchesQuery = !q || card.dataset.search.includes(q);
    const visible = matchesCat && matchesQuery;
    card.hidden = !visible;
    if (visible) shown++;
  }
  const total = moduleBrowser.cards.length;
  $("#modules-shown").textContent = `${shown} / ${total} module${total !== 1 ? "s" : ""}`;
  $("#modules-empty").hidden = shown !== 0;
}

function resetFilters(focusSearch = false) {
  const searchInput = $("#module-search");
  searchInput.value = "";
  moduleBrowser.query = "";
  moduleBrowser.category = "all";
  $$("#module-tabs .seg-btn").forEach((b) => {
    const active = b.dataset.cat === "all";
    b.classList.toggle("active", active);
    b.setAttribute("aria-pressed", String(active));
  });
  $("#search-clear").hidden = true;
  applyModuleFilters();
  if (focusSearch) searchInput.focus();
}

function gotoModule(slug) {
  const module = moduleBrowser.modulesBySlug[slug];
  if (module) openModuleDialog(module);
}

// Feature chips / showcase links jumping to modules
document.addEventListener("click", (e) => {
  const trigger = e.target.closest("[data-goto-module]");
  if (trigger) gotoModule(trigger.dataset.gotoModule);
});

// "/" focuses module search
document.addEventListener("keydown", (e) => {
  if (e.key !== "/" || e.ctrlKey || e.metaKey || e.altKey) return;
  const tag = document.activeElement?.tagName;
  if (tag === "INPUT" || tag === "TEXTAREA" || document.activeElement?.isContentEditable) return;
  if ($("#install-popover")?.matches(":popover-open")) return;
  e.preventDefault();
  const searchInput = $("#module-search");
  searchInput.focus();
  searchInput.scrollIntoView({ behavior: "smooth", block: "center" });
});

function renderModuleBrowser(data) {
  const grid = $("#modules-grid");
  const modules = data.modules;

  const frag = document.createDocumentFragment();

  for (const module of modules) {
    const slug = module.name;
    const pretty = humanize(module.name);
    const settingsCount = countSettings(module);

    const card = el("button", "module-card");
    card.type = "button";
    card.id = `module-${slug}`;
    card.dataset.cat = module.category;
    card.dataset.search = moduleSearchIndex(module);

    const head = el("div", "module-head");
    const icon = el("span", "material-symbols-rounded module-icon", "extension");
    icon.setAttribute("aria-hidden", "true");
    head.appendChild(icon);
    head.appendChild(el("span", "module-name", pretty));
    if (module.category !== "Meteorist") {
      head.appendChild(el("span", "module-cat", "dev"));
    }
    head.appendChild(el("span", "module-count", `${settingsCount} setting${settingsCount !== 1 ? "s" : ""}`));
    const chevron = el("span", "material-symbols-rounded module-chevron", "chevron_right");
    chevron.setAttribute("aria-hidden", "true");
    head.appendChild(chevron);
    card.appendChild(head);

    card.appendChild(el("span", "module-desc", unescapeDoubleQuotes(module.description || "")));
    card.setAttribute("aria-label", `${pretty}: open settings`);
    card.addEventListener("click", () => openModuleDialog(module));

    moduleBrowser.cards.push(card);
    frag.appendChild(card);
  }

  grid.appendChild(frag);

  moduleBrowser.modulesBySlug = {};
  modules.forEach((m) => {
    moduleBrowser.modulesBySlug[m.name] = m;
  });

  // Category counts
  const counts = { all: modules.length, Meteorist: 0, MeteoristDev: 0 };
  modules.forEach((m) => {
    counts[m.category] = (counts[m.category] || 0) + 1;
  });
  $$("#module-tabs .seg-count").forEach((node) => {
    node.textContent = counts[node.dataset.countFor] ?? "";
  });

  applyModuleFilters();

  // Deep links: #module-<slug> (fresh load + in-page hash changes)
  handleModuleHash();
}

function handleModuleHash() {
  const hash = decodeURIComponent(location.hash || "");
  if (!hash.startsWith("#module-")) return;
  const slug = hash.slice("#module-".length);
  if (!moduleBrowser.cards.length) return; // not rendered yet
  requestAnimationFrame(() => gotoModule(slug));
}

// Section anchors (e.g. #hud) sit below content that is rendered
// asynchronously, so the browser's initial jump lands at a stale offset.
// Re-scroll while the layout grows, until it settles or the user scrolls.
let anchorAutoScroll = true;
window.addEventListener("wheel", () => (anchorAutoScroll = false), { passive: true });
window.addEventListener("touchmove", () => (anchorAutoScroll = false), { passive: true });

let anchorObserver = null;
let anchorSettleTimer = null;

function scrollToAnchor() {
  if (!anchorAutoScroll) return;
  const hash = decodeURIComponent(location.hash || "");
  if (!hash || hash.startsWith("#module-")) return;
  document.getElementById(hash.slice(1))?.scrollIntoView({ behavior: "instant" });
}

function keepAnchorInView() {
  scrollToAnchor();
  clearTimeout(anchorSettleTimer);
  anchorSettleTimer = setTimeout(() => {
    anchorObserver?.disconnect();
    anchorObserver = null;
  }, 600);
}

function watchAnchor() {
  if (!location.hash || location.hash.startsWith("#module-")) return;
  anchorObserver ??= new ResizeObserver(keepAnchorInView);
  anchorObserver.observe(document.body);
  keepAnchorInView();
}

window.addEventListener("hashchange", handleModuleHash);
window.addEventListener("load", scrollToAnchor);
watchAnchor();

function makeCopyCard({ icon, titleNode, description, onCopy, extra, compact }) {
  const card = el("button", compact ? "copy-card compact" : "copy-card");
  card.type = "button";

  const head = el("div", "copy-head");
  if (icon) {
    const lead = el("span", "material-symbols-rounded lead", icon);
    lead.setAttribute("aria-hidden", "true");
    head.appendChild(lead);
  }
  head.appendChild(titleNode);
  const hint = el("span", "copy-hint");
  const hintIcon = el("span", "material-symbols-rounded", "content_copy");
  hintIcon.setAttribute("aria-hidden", "true");
  hint.appendChild(hintIcon);
  hint.appendChild(document.createTextNode(" copy"));
  head.appendChild(hint);
  card.appendChild(head);

  if (description) card.appendChild(el("p", "copy-desc", description));
  if (extra) card.appendChild(extra);

  card.addEventListener("click", async () => {
    const ok = await copyText(onCopy);
    if (ok) {
      showToast(`Copied ${onCopy.length > 40 ? onCopy.slice(0, 40) + "..." : onCopy}`);
      card.classList.add("copied");
      setTimeout(() => card.classList.remove("copied"), 1200);
    }
  });
  return card;
}

function renderCommands(commands) {
  const grid = $("#commands-grid");
  const frag = document.createDocumentFragment();

  for (const cmd of commands) {
    const pretty = `.${cmd.name}`;
    const usageCount = (cmd.usage || []).length;

    const card = el("button", "module-card");
    card.type = "button";

    const head = el("div", "module-head");
    const icon = el("span", "material-symbols-rounded module-icon", "terminal");
    icon.setAttribute("aria-hidden", "true");
    head.appendChild(icon);
    head.appendChild(el("span", "module-name", pretty));
    head.appendChild(el("span", "module-count", `${usageCount} command${usageCount !== 1 ? "s" : ""}`));
    const chevron = el("span", "material-symbols-rounded module-chevron", "chevron_right");
    chevron.setAttribute("aria-hidden", "true");
    head.appendChild(chevron);
    card.appendChild(head);

    card.appendChild(el("span", "module-desc", unescapeDoubleQuotes(cmd.description || "")));
    card.setAttribute("aria-label", `${pretty}: open command`);
    card.addEventListener("click", () => openCommandDialog(cmd));

    frag.appendChild(card);
  }

  grid.appendChild(frag);
}

const COMMAND_ARG_COLORS = ["#8ec07c", "#e5b567", "#7aa2f7", "#c678dd"];

function commandTitleNode(full) {
  const node = el("span", "copy-title command-text");
  const tokens = full.split(" ");
  tokens.forEach((token, index) => {
    if (index === 0) {
      node.appendChild(document.createTextNode(token));
    } else {
      const arg = el("span", null, token);
      arg.style.color = COMMAND_ARG_COLORS[(index - 1) % COMMAND_ARG_COLORS.length];
      node.appendChild(arg);
    }
    if (index < tokens.length - 1) node.appendChild(document.createTextNode(" "));
  });
  return node;
}

function openCommandDialog(cmd) {
  if (!moduleDialog) return;
  moduleDialog.textContent = "";

  const close = el("button", "dialog-close");
  close.type = "button";
  close.setAttribute("aria-label", "Close");
  close.appendChild(el("span", "material-symbols-rounded", "close"));
  close.addEventListener("click", () => moduleDialog.close());
  moduleDialog.appendChild(close);

  const head = el("div", "dialog-head");
  const icon = el("span", "material-symbols-rounded module-icon", "terminal");
  icon.setAttribute("aria-hidden", "true");
  head.appendChild(icon);
  const title = el("h3", "dialog-title", `.${cmd.name}`);
  title.id = "module-dialog-title";
  head.appendChild(title);

  const copy = el("button", "dialog-copy");
  copy.type = "button";
  copy.setAttribute("aria-label", "Copy command");
  const copyIcon = el("span", "material-symbols-rounded", "content_copy");
  copyIcon.setAttribute("aria-hidden", "true");
  copy.appendChild(copyIcon);
  copy.addEventListener("click", async () => {
    const ok = await copyText(`.${cmd.name}`);
    if (ok) {
      showToast(`Copied .${cmd.name}`);
      copy.classList.add("copied");
      setTimeout(() => copy.classList.remove("copied"), 1200);
    }
  });
  head.appendChild(copy);

  moduleDialog.appendChild(head);

  const aliases = cmd.aliases || [];
  const usageCount = (cmd.usage || []).length;
  if (aliases.length) {
    const meta = el("div", "dialog-meta");
    aliases.forEach((alias) => meta.appendChild(el("span", "alias-chip", `.${alias.replace(/^\.?/, "")}`)));
    moduleDialog.appendChild(meta);
  }

  moduleDialog.appendChild(el("p", "dialog-desc", unescapeDoubleQuotes(cmd.description || "")));

  if (cmd.classPath) {
    const src = el("a", "dialog-source");
    const classPath = String(cmd.classPath).replace(/\\/g, "/");
    src.href = `https://github.com/Zgoly/Meteorist/blob/main/${classPath}#L${cmd.lineNumber || 1}`;
    src.target = "_blank";
    src.rel = "noopener";
    const srcIcon = el("span", "material-symbols-rounded small-icon", "code");
    srcIcon.setAttribute("aria-hidden", "true");
    src.appendChild(srcIcon);
    src.appendChild(el("span", "dialog-source-label", "View source on GitHub"));
    moduleDialog.appendChild(src);
  }

  const usage = cmd.usage || [];
  const parameters = cmd.parameters || [];
  if (parameters.length || usage.length) {
    const box = el("div", "dialog-settings");
    if (parameters.length) {
      box.appendChild(el("p", "dialog-settings-title", "Parameters"));
      const params = el("div", "cmd-aliases");
      parameters.forEach((p) => params.appendChild(el("span", "alias-chip", `${p.name}: ${p.type}`)));
      box.appendChild(params);
    }
    box.appendChild(el("p", "dialog-settings-title", `Commands (${usageCount})`));
    for (const line of usage) {
      const full = `.${cmd.name} ${line}`;
      box.appendChild(makeCopyCard({ titleNode: commandTitleNode(full), onCopy: full, compact: true }));
    }
    moduleDialog.appendChild(box);
  }

  if (typeof moduleDialog.showModal === "function") moduleDialog.showModal();
  else moduleDialog.setAttribute("open", "");
}

// Starscript stuff
const SS_KEYWORDS = new Set(["null", "true", "false", "and", "or"]);
const SS_OPERATORS = "=!<>+-*/%^?:&|~";
// Default Meteor HUD section colors (Hud -> text-colors).
const SS_SECTIONS = ["#ffffff", "#afafaf", "#19e119", "#e11919"];

function isDigit(ch) {
  return ch >= "0" && ch <= "9";
}

function isIdentStart(ch) {
  return (ch >= "a" && ch <= "z") || (ch >= "A" && ch <= "Z") || ch === "_";
}

// Splits Starscript source into consecutive { cls, text, color } spans.
// cls is null for literal text, color is only used for '#' sections.
function highlightStarscript(source) {
  const spans = [];
  let plain = "";
  const flush = () => {
    if (plain) {
      spans.push({ cls: null, text: plain });
      plain = "";
    }
  };
  const token = (cls, text, color) => {
    flush();
    const last = spans[spans.length - 1];
    if (last && last.cls === cls && last.color === color) last.text += text;
    else spans.push({ cls, text, color });
  };
  const section = (start, end) => {
    token("ss-section", source.slice(start, end), SS_SECTIONS[Number(source.slice(start + 1, end))] || SS_SECTIONS[0]);
  };

  let i = 0;
  let depth = 0;

  while (i < source.length) {
    const ch = source[i++];

    if (depth === 0) {
      if (ch === "#" && isDigit(source[i])) {
        const start = i;
        while (isDigit(source[i])) i++;
        section(start - 1, i);
      } else if (ch === "{" && source[i] !== "{") {
        token("ss-brace", ch);
        depth++;
      } else {
        plain += ch;
        if ((ch === "{" || ch === "#") && source[i] === ch) plain += source[i++];
      }
    } else if (ch === "{") {
      token("ss-brace", ch);
      depth++;
    } else if (ch === "}") {
      token("ss-brace", ch);
      depth--;
    } else if (ch === "(" || ch === ")" || ch === "," || ch === ".") {
      token("ss-text", ch);
    } else if (ch === "#" && isDigit(source[i])) {
      const start = i;
      while (isDigit(source[i])) i++;
      section(start - 1, i);
    } else if (ch === '"' || ch === "'") {
      const start = i - 1;
      while (i < source.length && source[i] !== ch) i += source[i] === "\\" ? 2 : 1;
      if (i < source.length) i++;
      token("ss-string", source.slice(start, i));
    } else if (isDigit(ch) || (ch === "-" && isDigit(source[i]))) {
      const start = i - 1;
      while (isDigit(source[i])) i++;
      if (source[i] === "." && isDigit(source[i + 1])) {
        i++;
        while (isDigit(source[i])) i++;
      }
      token("ss-number", source.slice(start, i));
    } else if (isIdentStart(ch)) {
      const start = i - 1;
      while (i < source.length && (isIdentStart(source[i]) || isDigit(source[i]))) i++;
      const word = source.slice(start, i);
      if (SS_KEYWORDS.has(word)) {
        token("ss-keyword", word);
      } else {
        let j = i;
        while (j < source.length && " \t\r\n".includes(source[j])) j++;
        token(source[j] === "." ? "ss-map" : "ss-text", word);
      }
    } else if (SS_OPERATORS.includes(ch)) {
      const start = i - 1;
      while (SS_OPERATORS.includes(source[i])) i++;
      token("ss-text", source.slice(start, i));
    } else {
      plain += ch;
    }
  }

  flush();
  return spans;
}

function starscriptNode(source, className) {
  const node = el("code", className);
  for (const span of highlightStarscript(source)) {
    if (!span.cls) {
      node.appendChild(document.createTextNode(span.text));
      continue;
    }
    const child = el("span", span.cls, span.text);
    if (span.color) child.style.color = span.color;
    node.appendChild(child);
  }
  return node;
}

function renderHudElements(elements) {
  const grid = $("#hud-grid");
  if (!grid) return;

  const frag = document.createDocumentFragment();

  for (const element of elements) {
    const pretty = humanize(element.name);
    const settingsCount = countSettings(element);
    const presetsCount = (element.presets || []).length;
    let countText;
    if (settingsCount) countText = `${settingsCount} setting${settingsCount !== 1 ? "s" : ""}`;
    else if (presetsCount) countText = `${presetsCount} HUD preset${presetsCount !== 1 ? "s" : ""}`;
    else countText = "No settings";

    const card = el("button", "module-card hud-card");
    card.type = "button";
    card.id = `hud-${element.name}`;

    const head = el("div", "module-head");
    const icon = el("span", "material-symbols-rounded module-icon", "tv");
    icon.setAttribute("aria-hidden", "true");
    head.appendChild(icon);
    head.appendChild(el("span", "module-name", pretty));
    head.appendChild(el("span", "module-count", countText));
    const chevron = el("span", "material-symbols-rounded module-chevron", "chevron_right");
    chevron.setAttribute("aria-hidden", "true");
    head.appendChild(chevron);
    card.appendChild(head);

    card.appendChild(el("span", "module-desc", unescapeDoubleQuotes(element.description || "")));
    card.setAttribute("aria-label", `${pretty}: open element`);
    card.addEventListener("click", () => openModuleDialog(element));
    frag.appendChild(card);
  }

  grid.appendChild(frag);
}

async function loadAndRenderContent() {
  try {
    const res = await fetch("generated/meteorist-info.json");
    const data = await res.json();

    renderModuleBrowser(data);
    renderHudElements(data.hud || []);
    renderCommands(data.commands || []);

    const totalSettings = (data.modules || []).reduce((sum, m) => sum + countSettings(m), 0);
    $$(".js-module-count").forEach((node) => {
      node.textContent = data.modules?.length ?? node.textContent;
    });
    $$(".js-setting-count").forEach((node) => {
      node.textContent = totalSettings || node.textContent;
    });

    keepAnchorInView();
  } catch (error) {
    console.error("Error loading content:", error);
    const grid = $("#modules-grid");
    if (grid) {
      grid.appendChild(el("p", "dim", "Couldn't load module data. Check the GitHub repository for the full list."));
    }
  }
}

loadAndRenderContent();

const searchInput = $("#module-search");
const searchClear = $("#search-clear");

searchInput?.addEventListener("input", () => {
  moduleBrowser.query = searchInput.value;
  searchClear.hidden = !searchInput.value;
  applyModuleFilters();
});

searchClear?.addEventListener("click", () => {
  searchInput.value = "";
  moduleBrowser.query = "";
  searchClear.hidden = true;
  applyModuleFilters();
  searchInput.focus();
});

$$("#module-tabs .seg-btn").forEach((btn) => {
  btn.addEventListener("click", () => {
    moduleBrowser.category = btn.dataset.cat;
    $$("#module-tabs .seg-btn").forEach((b) => {
      const active = b === btn;
      b.classList.toggle("active", active);
      b.setAttribute("aria-pressed", String(active));
    });
    applyModuleFilters();
  });
});

$("#modules-empty-clear")?.addEventListener("click", () => resetFilters(true));
