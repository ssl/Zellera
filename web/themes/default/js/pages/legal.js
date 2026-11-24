'use strict';

const legalPage = location.pathname === '/legal/privacy' ? 'privacy' : 'terms';
document.body.dataset.sub = legalPage;
Site.start((main) => Site.load(main, `/legal/${legalPage}`, (r) => siteDoc(main, r)));
