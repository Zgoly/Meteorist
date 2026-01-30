const isDebug = false;

function unescapeDoubleQuotes(str) {
    if (typeof str !== 'string') return str;
    return str.split('\\"').join('"');
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function getTimeAgo(dateString) {
    const date = new Date(dateString);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);

    const intervals = [
        { label: 'year', seconds: 31536000 },
        { label: 'month', seconds: 2592000 },
        { label: 'week', seconds: 604800 },
        { label: 'day', seconds: 86400 },
        { label: 'hour', seconds: 3600 },
        { label: 'minute', seconds: 60 }
    ];

    for (const interval of intervals) {
        const count = Math.floor(seconds / interval.seconds);
        if (count > 0) {
            return `${count} ${interval.label}${count !== 1 ? 's' : ''} ago`;
        }
    }

    return 'Just now';
}

async function loadAndRenderContent() {
    try {
        const response = await fetch('generated/meteorist-info.json');
        const data = await response.json();

        const modulesContainer = document.getElementById('modules-grid');
        const commandsContainer = document.getElementById('commands-grid');
        const presetsContainer = document.getElementById('presets-grid');

        const groups = {};
        data.modules.forEach(module => {
            const cat = module.category;
            if (!groups[cat]) groups[cat] = [];
            groups[cat].push(module);
        });

        Object.keys(groups).sort().forEach(cat => {
            const section = document.createElement('div');
            section.classList.add('category-section');

            const title = document.createElement('h3');
            title.classList.add('category-title');

            const icon = document.createElement('span');
            icon.classList.add('material-symbols-rounded');
            icon.textContent = 'category';
            title.appendChild(icon);

            title.appendChild(document.createTextNode(cat))

            section.appendChild(title);

            const grid = document.createElement('div');
            grid.classList.add('content-grid');

            groups[cat].forEach(module => {
                const name = unescapeDoubleQuotes(module.name).replace(/-/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
                const card = document.createElement('div');
                card.classList.add('content-card');

                const title = document.createElement('h4');
                title.classList.add('content-title');

                const icon = document.createElement('span');
                icon.classList.add('material-symbols-rounded');
                icon.textContent = 'extension';
                title.appendChild(icon);

                title.appendChild(document.createTextNode(name))

                const desc = document.createElement('p');
                desc.classList.add('content-description');
                desc.textContent = unescapeDoubleQuotes(module.description);

                card.appendChild(title);
                card.appendChild(desc);
                grid.appendChild(card);
            });

            section.appendChild(grid);
            modulesContainer.appendChild(section);
        });

        data.commands.forEach(cmd => {
            const card = document.createElement('div');
            card.classList.add('content-card');

            const title = document.createElement('h4');
            title.classList.add('content-title');

            const icon = document.createElement('span');
            icon.classList.add('material-symbols-rounded');
            icon.textContent = 'terminal';
            title.appendChild(icon);

            title.appendChild(document.createTextNode(`/${cmd.name}`))
            card.appendChild(title);

            const desc = document.createElement('p');
            desc.classList.add('content-description');
            desc.textContent = unescapeDoubleQuotes(cmd.description);

            if (cmd.aliases && cmd.aliases.length > 0) {
                const aliasText = document.createTextNode(' — Aliases: ');
                desc.appendChild(aliasText);

                const aliasesSpan = document.createElement('span');
                aliasesSpan.textContent = cmd.aliases.join(', ');
                desc.appendChild(aliasesSpan);
            }

            card.appendChild(desc);
            commandsContainer.appendChild(card);
        });

        const presets = data.presets && data.presets[0] && data.presets[0].presets ? data.presets[0].presets : [];
        presets.forEach(preset => {
            const card = document.createElement('div');
            card.classList.add('content-card');

            const title = document.createElement('h4');
            title.classList.add('content-title');

            const icon = document.createElement('span');
            icon.classList.add('material-symbols-rounded');
            icon.textContent = 'tv';
            title.appendChild(icon);

            title.appendChild(document.createTextNode(preset.title))
            card.appendChild(title);

            const desc = document.createElement('p');
            desc.textContent = unescapeDoubleQuotes(preset.text);
            desc.classList.add("content-description");

            card.appendChild(desc);
            presetsContainer.appendChild(card);
        });
    } catch (error) {
        console.error('Error loading content:', error);
    }
}

loadAndRenderContent()

async function setupReleaseInfo() {
    try {
        const response = await fetch('generated/release-info.json');
        const data = await response.json();

        if (!data.url || !data.tag_name) return;

        const downloadLink = document.getElementById('download-link');
        downloadLink.href = data.url;

        const tagNameSpans = document.querySelectorAll('.tag-name');
        tagNameSpans.forEach(span => span.textContent = data.tag_name);

        const fileSizeSpans = document.querySelectorAll('.file-size');
        fileSizeSpans.forEach(span => span.textContent = formatFileSize(data.size));

        const timeAgoSpans = document.querySelectorAll('.time-ago');
        timeAgoSpans.forEach(span => span.textContent = getTimeAgo(data.updated_at));

        const downloadBtn = document.getElementById('download-btn');

        if (!isDebug) downloadBtn.addEventListener('click', e => {
            const link = document.createElement('a');
            link.href = data.url;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        });

    } catch (error) {
        console.error('Failed to load release info:', error);
    }
}

setupReleaseInfo();

async function parseShieldsIoInfo() {
    const parser = new DOMParser();

    async function fetchBadgeText(url, validElementId, invalidContainerId) {
        try {
            const res = await fetch(url);
            const svgText = await res.text();
            const doc = parser.parseFromString(svgText, 'image/svg+xml');
            const text = doc.querySelector('text')?.textContent?.trim();

            if (text && text !== 'invalid') {
                document.getElementById(validElementId).textContent = text;
            } else {
                // Rate-limited or invalid
                document.getElementById(invalidContainerId)?.remove();
            }
        } catch (e) {
            console.warn(`Failed to fetch badge from ${url}`, e);
        }
    }

    await Promise.all([
        fetchBadgeText(
            'https://img.shields.io/github/stars/Zgoly/Meteorist?style=flat-square&label=',
            'github-star-count',
            'github-star-badge'
        ),
        fetchBadgeText(
            'https://img.shields.io/github/downloads/Zgoly/Meteorist/total?style=flat-square&label=',
            'hero-download-counter-number',
            'hero-download-counter'
        )
    ]);
}

parseShieldsIoInfo()

const sections = document.querySelectorAll('section');

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        entry.target.classList.toggle('scrolled', entry.isIntersecting);
    });
});

sections.forEach(section => observer.observe(section));