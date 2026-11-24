'use strict';

Site.start((main) => Site.showError(main, { status: 404, data: { title: 'Page not found', error: 'This page does not exist, or it is gone.' } }));
