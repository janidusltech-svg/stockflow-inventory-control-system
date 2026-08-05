(() => {
    "use strict";

    const ready = (callback) => {
        if (document.readyState === "loading") {
            document.addEventListener("DOMContentLoaded", callback, { once: true });
        } else {
            callback();
        }
    };

    document.documentElement.classList.add("ui-ready");

    ready(() => {
        initialiseSidebar();
        initialisePageContext();
        initialiseAlerts();
        initialiseConfirmations();
        initialiseTablePagination();
        initialiseRevealEffects();
    });

    function initialiseSidebar() {
        const openButton = document.querySelector("[data-sidebar-open]");
        const closeButtons = document.querySelectorAll("[data-sidebar-close]");
        const sidebar = document.querySelector("#app-sidebar");

        if (!sidebar) {
            return;
        }

        sidebar.querySelectorAll(".nav-links a.active")
            .forEach((link) => link.setAttribute("aria-current", "page"));

        const open = () => {
            document.body.classList.add("sidebar-open");
            openButton?.setAttribute("aria-expanded", "true");
        };
        const close = () => {
            document.body.classList.remove("sidebar-open");
            openButton?.setAttribute("aria-expanded", "false");
        };

        openButton?.setAttribute("aria-controls", "app-sidebar");
        openButton?.setAttribute("aria-expanded", "false");
        openButton?.addEventListener("click", open);
        closeButtons.forEach((button) => button.addEventListener("click", close));
        sidebar.querySelectorAll("a").forEach((link) => link.addEventListener("click", close));

        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                close();
            }
        });
    }

    function initialisePageContext() {
        const page = document.querySelector("main.page");
        const heading = page?.querySelector("h1");

        if (!page || !heading || page.querySelector(":scope > .page-context-bar")) {
            return;
        }

        const documentTitle = document.title.split("|")[0].trim();
        const currentTitle = documentTitle
            || heading.textContent.replace(/\s+/g, " ").trim()
            || "Current page";
        const contextBar = document.createElement("div");
        contextBar.className = "page-context-bar";
        contextBar.innerHTML = `
            <nav class="breadcrumb" aria-label="Breadcrumb">
                <a href="/dashboard">Dashboard</a>
                <span class="breadcrumb-separator" aria-hidden="true">/</span>
                <span aria-current="page"></span>
            </nav>
            <span class="page-security-state">Secured role access</span>
        `;
        contextBar.querySelector("[aria-current='page']").textContent = currentTitle;
        page.prepend(contextBar);

        if (document.querySelector("#app-sidebar .view-mode")) {
            const banner = document.createElement("div");
            banner.className = "read-only-banner";
            banner.setAttribute("role", "status");
            banner.innerHTML = "<strong>View-only access</strong><span>This role can inspect information on this page, but record-changing actions are restricted.</span>";
            contextBar.insertAdjacentElement("afterend", banner);
        }
    }

    function initialiseAlerts() {
        document.querySelectorAll(".alert").forEach((alert) => {
            if (alert.querySelector(".alert-dismiss")) {
                return;
            }

            const button = document.createElement("button");
            button.type = "button";
            button.className = "alert-dismiss";
            button.setAttribute("aria-label", "Dismiss message");
            button.textContent = "×";
            button.addEventListener("click", () => alert.remove());
            alert.append(button);
        });
    }

    function initialiseConfirmations() {
        const dialog = document.querySelector("#confirm-dialog");
        const cancelButton = dialog?.querySelector("[data-confirm-cancel]");
        const acceptButton = dialog?.querySelector("[data-confirm-accept]");
        const message = dialog?.querySelector("#confirm-dialog-message");
        const destructivePattern = /\b(confirm|approve|deactivate|activate|cancel|delete|close|dispatch|receive|submit|reset password|update stock|deduct stock)\b/i;
        let pendingForm = null;
        let pendingButton = null;

        if (!dialog || !cancelButton || !acceptButton || !message) {
            return;
        }

        const close = () => {
            dialog.hidden = true;
            document.body.classList.remove("dialog-open");
            pendingForm = null;
            pendingButton = null;
        };

        const continueSubmission = () => {
            const form = pendingForm;
            const button = pendingButton;
            close();
            if (!form) {
                return;
            }
            form.dataset.confirmed = "true";
            form.requestSubmit(button || undefined);
        };

        document.addEventListener("submit", (event) => {
            const form = event.target;
            if (!(form instanceof HTMLFormElement)) {
                return;
            }
            if (form.dataset.confirmed === "true") {
                delete form.dataset.confirmed;
                return;
            }

            const submitter = event.submitter;
            const label = (submitter?.textContent || submitter?.value || "").replace(/\s+/g, " ").trim();
            const requiresConfirmation = form.dataset.confirm === "true"
                || submitter?.dataset.confirm === "true"
                || submitter?.classList.contains("danger")
                || destructivePattern.test(label);

            if (!requiresConfirmation || form.dataset.noConfirm === "true") {
                return;
            }

            event.preventDefault();
            pendingForm = form;
            pendingButton = submitter instanceof HTMLElement ? submitter : null;
            message.textContent = label
                ? `Please confirm “${label}”. The system will validate the request before updating its records.`
                : "Please confirm this action. The system will validate the request before updating its records.";
            dialog.hidden = false;
            document.body.classList.add("dialog-open");
            cancelButton.focus();
        });

        cancelButton.addEventListener("click", close);
        acceptButton.addEventListener("click", continueSubmission);
        dialog.addEventListener("click", (event) => {
            if (event.target === dialog) {
                close();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && !dialog.hidden) {
                close();
            }
        });
    }

    function initialiseTablePagination() {
        const pageSize = 10;

        document.querySelectorAll(".table-panel table").forEach((table, tableIndex) => {
            const body = table.tBodies[0];
            if (!body) {
                return;
            }

            const rows = Array.from(body.rows).filter((row) => !row.querySelector(".empty, .empty-state"));
            if (rows.length <= pageSize) {
                return;
            }

            const pageCount = Math.ceil(rows.length / pageSize);
            let currentPage = 1;
            const pagination = document.createElement("div");
            pagination.className = "table-pagination";
            pagination.setAttribute("aria-label", `Table ${tableIndex + 1} pagination`);
            const summary = document.createElement("span");
            summary.className = "pagination-summary";
            const controls = document.createElement("div");
            controls.className = "pagination-actions";
            pagination.append(summary, controls);

            const createButton = (label, page, ariaLabel) => {
                const button = document.createElement("button");
                button.type = "button";
                button.className = "pagination-button";
                button.textContent = label;
                button.setAttribute("aria-label", ariaLabel);
                button.addEventListener("click", () => render(page));
                return button;
            };

            const render = (requestedPage) => {
                currentPage = Math.max(1, Math.min(requestedPage, pageCount));
                const start = (currentPage - 1) * pageSize;
                const end = Math.min(start + pageSize, rows.length);
                rows.forEach((row, index) => {
                    row.hidden = index < start || index >= end;
                });

                summary.textContent = `Showing ${start + 1}–${end} of ${rows.length} records`;
                controls.replaceChildren();
                const previous = createButton("‹", currentPage - 1, "Previous page");
                previous.disabled = currentPage === 1;
                controls.append(previous);

                for (let page = 1; page <= pageCount; page += 1) {
                    const pageButton = createButton(String(page), page, `Page ${page}`);
                    pageButton.classList.toggle("active", page === currentPage);
                    pageButton.setAttribute("aria-current", page === currentPage ? "page" : "false");
                    controls.append(pageButton);
                }

                const next = createButton("›", currentPage + 1, "Next page");
                next.disabled = currentPage === pageCount;
                controls.append(next);
            };

            const scrollContainer = table.closest(".table-scroll");
            (scrollContainer || table).insertAdjacentElement("afterend", pagination);
            render(1);
        });
    }

    function initialiseRevealEffects() {
        const elements = document.querySelectorAll([
            ".page-header",
            ".hero",
            ".dashboard-hero",
            ".dashboard-section-heading",
            ".summary-grid",
            ".module-grid",
            ".table-panel",
            ".form-panel",
            ".workflow-panel",
            ".workflow-overview",
            ".content-section",
            ".reference-policy",
            ".reference-section",
            ".friendly-error-card"
        ].join(","));

        elements.forEach((element) => element.classList.add("ui-reveal"));

        if (!("IntersectionObserver" in window)) {
            elements.forEach((element) => element.classList.add("is-visible"));
            return;
        }

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.08, rootMargin: "0px 0px -24px" });

        elements.forEach((element) => observer.observe(element));
    }
})();
