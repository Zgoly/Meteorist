const isDebug = false;

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
                const name = module.name.replace(/-/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
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
                desc.textContent = module.description;

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
            desc.textContent = cmd.description;

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
            desc.textContent = preset.text;
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

        const downloadBtn = document.getElementById('download-btn');
        if (!downloadBtn || !data.tag_name || !data.assets?.length) return;

        const jarAsset = data.assets.find(asset => asset.name.endsWith('.jar'));
        if (!jarAsset) return;

        const icon = document.createElement('span');
        icon.className = 'material-symbols-rounded';
        icon.textContent = 'download';

        downloadBtn.textContent = `Download ${data.tag_name}`;
        downloadBtn.insertBefore(icon, downloadBtn.firstChild);

        const downloadLink = document.getElementById('download-link');
        downloadLink.href = jarAsset.browser_download_url;

        if (!isDebug) downloadBtn.addEventListener('click', e => {
            const link = document.createElement('a');
            link.href = jarAsset.browser_download_url;
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

const sections = document.querySelectorAll('section');

const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    entry.target.classList.toggle('scrolled', entry.isIntersecting);
  });
});

sections.forEach(section => observer.observe(section));