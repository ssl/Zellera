'use strict';

Site.start((main) => Site.load(main, '/safety', (r) => siteDoc(main, r)));
