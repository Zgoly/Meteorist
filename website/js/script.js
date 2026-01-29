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
            section.className = 'category-section';

            const title = document.createElement('h3');
            title.className = 'category-title';
            title.textContent = cat;
            section.appendChild(title);

            const grid = document.createElement('div');
            grid.className = 'content-grid';

            groups[cat].forEach(module => {
                const name = unescapeDoubleQuotes(module.name).replace(/-/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
                const card = document.createElement('div');
                card.className = 'content-card';

                const titleDiv = document.createElement('div');
                titleDiv.className = 'content-title';

                const icon = document.createElement('span');
                icon.className = 'material-symbols-rounded';
                icon.textContent = 'extension';
                titleDiv.appendChild(icon);

                const h4 = document.createElement('h4');
                h4.textContent = name;
                titleDiv.appendChild(h4);

                const desc = document.createElement('p');
                desc.className = 'content-description';
                desc.textContent = unescapeDoubleQuotes(module.description);

                card.appendChild(titleDiv);
                card.appendChild(desc);
                grid.appendChild(card);
            });

            section.appendChild(grid);
            modulesContainer.appendChild(section);
        });

        data.commands.forEach(cmd => {
            const card = document.createElement('div');
            card.className = 'content-card';

            const titleDiv = document.createElement('div');
            titleDiv.className = 'content-title';

            const icon = document.createElement('span');
            icon.className = 'material-symbols-rounded';
            icon.textContent = 'terminal';
            titleDiv.appendChild(icon);

            const h4 = document.createElement('h4');
            h4.textContent = `/${cmd.name}`;
            titleDiv.appendChild(h4);

            const desc = document.createElement('p');
            desc.className = 'content-description';
            desc.textContent = unescapeDoubleQuotes(cmd.description);

            if (cmd.aliases && cmd.aliases.length > 0) {
                const aliasText = document.createTextNode(' — Aliases: ');
                desc.appendChild(aliasText);

                const aliasesSpan = document.createElement('span');
                aliasesSpan.textContent = cmd.aliases.join(', ');
                desc.appendChild(aliasesSpan);
            }

            card.appendChild(titleDiv);
            card.appendChild(desc);
            commandsContainer.appendChild(card);
        });

        const presets = data.presets && data.presets[0] && data.presets[0].presets ? data.presets[0].presets : [];
        presets.forEach(preset => {
            const card = document.createElement('div');
            card.className = 'content-card';

            const titleDiv = document.createElement('div');
            titleDiv.className = 'content-title';

            const icon = document.createElement('span');
            icon.className = 'material-symbols-rounded';
            icon.textContent = 'tv';
            titleDiv.appendChild(icon);

            const h4 = document.createElement('h4');
            h4.textContent = preset.title;
            titleDiv.appendChild(h4);

            const desc = document.createElement('p');
            desc.textContent = unescapeDoubleQuotes(preset.text);
            desc.className = "content-description";

            card.appendChild(titleDiv);
            card.appendChild(desc);
            presetsContainer.appendChild(card);
        });
    } catch (error) {
        console.error('Error loading content:', error);
    }
}

loadAndRenderContent()

async function setupDownloadButton() {
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
            link.download = jarAsset.name;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        });

    } catch (error) {
        console.error('Failed to load release info:', error);
    }
}

setupDownloadButton();

async function parseShieldsIoInfo() {
    const btn = document.querySelector('.github-button');
    if (!btn) return;

    try {
        const starsRes = await fetch('https://img.shields.io/github/stars/Zgoly/Meteorist?style=flat-square&label=');
        const starsSvgText = await starsRes.text();

        const parser = new DOMParser();
        const starsDoc = parser.parseFromString(starsSvgText, 'image/svg+xml');
        const stars = starsDoc.querySelector('text')?.textContent?.trim();

        if (stars) {
            const badge = document.getElementById('github-star-count');
            badge.textContent = stars;
        }

        const downloadsRes = await fetch('https://img.shields.io/github/downloads/Zgoly/Meteorist/total?style=flat-square&label=');
        const downloadsSvgText = await downloadsRes.text();

        const downloadsDoc = parser.parseFromString(downloadsSvgText, 'image/svg+xml');
        const downloads = downloadsDoc.querySelector('text')?.textContent?.trim();

        if (downloads) {
            const downloadCounterNumber = document.getElementById('hero-download-counter-number');
            downloadCounterNumber.textContent = downloads;
        }
    } catch (e) {
        console.warn('Failed to parse stats', e);
    }
}

parseShieldsIoInfo()

const sections = document.querySelectorAll('section');

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        entry.target.classList.toggle('scrolled', entry.isIntersecting);
    });
});

sections.forEach(section => observer.observe(section));