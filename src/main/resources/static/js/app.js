/* Shared StockFlow workspace behavior. No business data is changed here. */
(function () {
    'use strict';

    function initialiseTablePagination() {
        document.querySelectorAll('.table-panel table').forEach(function (table) {
            var body = table.tBodies[0];
            if (!body || table.dataset.paginationReady === 'true') return;
            var rows = Array.from(body.rows);
            if (rows.length <= 10 || body.querySelector('input:not([type="hidden"]), select, textarea')) return;
            var panel = table.closest('.table-panel');
            if (!panel) return;

            table.dataset.paginationReady = 'true';
            var page = 0;
            var pages = Math.ceil(rows.length / 10);
            var bar = document.createElement('div');
            bar.className = 'table-pagination';
            var summary = document.createElement('span');
            summary.className = 'pagination-summary';
            var actions = document.createElement('div');
            actions.className = 'pagination-actions';
            var previous = document.createElement('button');
            var next = document.createElement('button');
            previous.type = next.type = 'button';
            previous.className = next.className = 'pagination-button';
            previous.textContent = 'Previous';
            next.textContent = 'Next';
            previous.setAttribute('aria-label', 'Previous table page');
            next.setAttribute('aria-label', 'Next table page');
            actions.append(previous, next);
            bar.append(summary, actions);
            panel.appendChild(bar);

            function showPage() {
                rows.forEach(function (row, index) {
                    row.hidden = index < page * 10 || index >= (page + 1) * 10;
                });
                summary.textContent = 'Showing ' + (page * 10 + 1) + '–' +
                    Math.min((page + 1) * 10, rows.length) + ' of ' + rows.length;
                previous.disabled = page === 0;
                next.disabled = page === pages - 1;
            }

            previous.addEventListener('click', function () {
                page--;
                showPage();
            });
            next.addEventListener('click', function () {
                page++;
                showPage();
            });
            showPage();
        });
    }

    function initialiseSidebar() {
        var toggle = document.querySelector('[data-sidebar-open]');
        if (!toggle) return;

        function close() {
            document.body.classList.remove('sidebar-open');
            toggle.setAttribute('aria-expanded', 'false');
        }

        toggle.setAttribute('aria-controls', 'app-sidebar');
        toggle.setAttribute('aria-expanded', 'false');
        toggle.addEventListener('click', function () {
            var open = document.body.classList.toggle('sidebar-open');
            toggle.setAttribute('aria-expanded', String(open));
        });

        document.querySelectorAll('[data-sidebar-close]').forEach(function (button) {
            button.addEventListener('click', close);
        });

        var sidebar = document.getElementById('app-sidebar');
        if (sidebar) {
            sidebar.addEventListener('click', function (event) {
                if (event.target.closest('a')) close();
            });
        }

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && document.body.classList.contains('sidebar-open')) {
                close();
                toggle.focus();
            }
        });
    }

    function initialiseConfirmations() {
        var dialog = document.getElementById('confirm-dialog');
        if (!dialog) return;

        var accept = dialog.querySelector('[data-confirm-accept]');
        var cancel = dialog.querySelector('[data-confirm-cancel]');
        var title = document.getElementById('confirm-dialog-title');
        var message = document.getElementById('confirm-dialog-message');
        if (!accept || !cancel) return;

        var pending = null;
        var previousFocus = null;

        function close() {
            dialog.hidden = true;
            document.body.classList.remove('dialog-open');
            pending = null;
            if (previousFocus && previousFocus.isConnected) previousFocus.focus();
        }

        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (!(form instanceof HTMLFormElement) || form.dataset.confirmed === 'true') {
                if (form instanceof HTMLFormElement) delete form.dataset.confirmed;
                return;
            }

            var path = new URL(form.action, window.location.href).pathname;
            var needsConfirmation = form.dataset.confirm === 'true' ||
                /\/(delete|deactivate|cancel|reject)$/.test(path);
            if (!needsConfirmation) return;

            event.preventDefault();
            pending = { form: form, submitter: event.submitter || null };
            previousFocus = event.submitter || document.activeElement;
            title.textContent = 'Continue with this action?';
            message.textContent = form.dataset.confirmMessage ||
                'Please check this action before changing a record.';
            dialog.hidden = false;
            document.body.classList.add('dialog-open');
            cancel.focus();
        });

        accept.addEventListener('click', function () {
            if (!pending) return;

            var form = pending.form;
            var submitter = pending.submitter;
            close();
            form.dataset.confirmed = 'true';

            try {
                if (submitter && submitter.form === form) form.requestSubmit(submitter);
                else form.requestSubmit();
            } finally {
                delete form.dataset.confirmed;
            }
        });

        cancel.addEventListener('click', close);
        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) close();
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !dialog.hidden) close();
        });
    }

    function initialiseNotices() {
        document.querySelectorAll('.alert').forEach(function (alert) {
            if (alert.querySelector('.alert-dismiss')) return;

            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'alert-dismiss';
            button.textContent = '×';
            button.setAttribute('aria-label', 'Dismiss notification');
            button.addEventListener('click', function () {
                alert.remove();
            });
            alert.appendChild(button);
        });
    }

    function initialisePageContext() {
        var page = document.querySelector('main.page');
        var heading = page && page.querySelector('.page-header h1');
        if (!heading || page.querySelector('.page-context-bar')) return;

        var context = document.createElement('div');
        context.className = 'page-context-bar';
        var breadcrumb = document.createElement('nav');
        breadcrumb.className = 'breadcrumb';
        breadcrumb.setAttribute('aria-label', 'Breadcrumb');

        var home = document.createElement('a');
        home.href = '/dashboard';
        home.textContent = 'Dashboard';

        var separator = document.createElement('span');
        separator.className = 'breadcrumb-separator';
        separator.setAttribute('aria-hidden', 'true');
        separator.textContent = '/';

        var current = document.createElement('span');
        current.textContent = heading.textContent.trim();

        breadcrumb.append(home, separator, current);
        context.appendChild(breadcrumb);

        var viewOnly = document.querySelector('.workspace-identity .view-mode');
        if (viewOnly) {
            var state = document.createElement('span');
            state.className = 'page-security-state';
            state.textContent = 'View only';
            context.appendChild(state);
        }

        page.insertBefore(context, page.firstChild);
    }

    function initialiseNotificationCount() {
        var badge = document.querySelector('[data-notification-count]');
        if (!badge || !window.fetch) return;

        fetch('/notifications/count', { credentials: 'same-origin' })
            .then(function (response) {
                return response.ok ? response.json() : null;
            })
            .then(function (data) {
                if (!data) return;
                var count = Number(data.count) || 0;
                badge.textContent = count > 99 ? '99+' : String(count);
                badge.hidden = count <= 0;
            })
            .catch(function () {
                badge.hidden = true;
            });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initialiseSidebar();
        initialiseTablePagination();
        initialiseConfirmations();
        initialiseNotices();
        initialisePageContext();
        initialiseNotificationCount();
    });
})();