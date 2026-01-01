package com.example.vaadinspringai;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.menu.MenuConfiguration;

import java.util.Locale;

@Layout
public class MainLayout extends AppLayout {

    public MainLayout() {
        UI.getCurrent().setLocale(Locale.ENGLISH);

        var head = new HorizontalLayout();
        head.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        head.add(new DrawerToggle());
        head.add(new H2("AI playground"));

        addToNavbar(head);

        var sideBar = new VerticalLayout();
        var links = new VerticalLayout();
        links.setMargin(false);

        MenuConfiguration.getMenuEntries().forEach(menuEntry -> {
            links.add(new RouterLink(menuEntry.title(), menuEntry.menuClass()));
        });

        var themeToggle = new Checkbox("Dark theme");

        themeToggle.addValueChangeListener(e -> {
            UI.getCurrentOrThrow().getPage().setColorScheme(e.getValue() ? ColorScheme.Value.DARK : ColorScheme.Value.LIGHT);
        });

        sideBar.addAndExpand(links);
        sideBar.add(themeToggle);

        addToDrawer(sideBar);



    }
}
