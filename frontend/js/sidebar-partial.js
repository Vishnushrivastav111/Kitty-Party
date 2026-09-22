document.addEventListener("DOMContentLoaded", function () {

    fetch("sidebar.html")

        .then(function (response) {

            if (!response.ok) {
                throw new Error(
                    "Sidebar file could not be loaded"
                );
            }

            return response.text();

        })

        .then(function (data) {

            document.getElementById(
                "sidebar-container"
            ).innerHTML = data;


            setActiveSidebar();

        })

        .catch(function (error) {

            console.error(
                "Sidebar could not be loaded:",
                error
            );

        });

});


function setActiveSidebar() {

    const currentPage =
        window.location.pathname.split("/").pop();


    const links =
        document.querySelectorAll(".sidebar a");


    links.forEach(function (link) {

        const linkPage =
            link.getAttribute("href");


        if (linkPage === currentPage) {

            link.parentElement.classList.add(
                "active"
            );

        }

    });

}