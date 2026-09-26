package com.masrdelivery.service;

import com.masrdelivery.config.PlatformConfig;
import com.masrdelivery.dispatch.*;
import com.masrdelivery.event.*;
import com.masrdelivery.menu.MenuItemFactory;
import com.masrdelivery.model.Customer;
import com.masrdelivery.model.Restaurant;
import com.masrdelivery.order.Order;
import com.masrdelivery.pricing.PricingEngine;
import com.masrdelivery.promotion.Promotion;
import com.masrdelivery.repo.InMemoryRepository;
import com.masrdelivery.report.ReportService;

import java.nio.file.Path;


public final class Platform {

    private final PlatformConfig config;
    private final MutableClock clock;

    private final InMemoryRepository<Restaurant> restaurants = new InMemoryRepository<>("Restaurant", Restaurant::id);
    private final InMemoryRepository<Customer> customers = new InMemoryRepository<>("Customer", Customer::id);
    private final InMemoryRepository<Rider> riders = new InMemoryRepository<>("Rider", Rider::id);
    private final InMemoryRepository<Order> orders = new InMemoryRepository<>("Order", Order::id);
    private final InMemoryRepository<Promotion> promotions = new InMemoryRepository<>("Promotion", Promotion::code);

    private final OrderEventBus events = new OrderEventBus();
    private final CustomerNotifier notifier = new CustomerNotifier();
    private final RiderDashboard riderDashboard = new RiderDashboard();
    private final AuditLog auditLog;
    private final PlatformStatistics statistics = new PlatformStatistics();

    private final MenuItemFactory menuItemFactory = MenuItemFactory.withDefaults();
    private final VehicleRegistry vehicles = VehicleRegistry.withDefaults();
    private final PricingEngine pricing;
    private final DispatchService dispatch;
    private final OrderService orderService;
    private final CatalogService catalog;
    private final ReportService reports;

    /** @param auditFile null keeps the audit log in memory only. */
    public Platform(PlatformConfig config, MutableClock clock, Path auditFile) {
        this.config = config;
        this.clock = clock;
        this.auditLog = new AuditLog(auditFile);
        events.subscribe(notifier);
        events.subscribe(riderDashboard);
        events.subscribe(auditLog);
        events.subscribe(statistics);

        this.pricing = new PricingEngine(config);
        this.dispatch = new DispatchService(new DispatchQueue(), riders, config.distances(), clock);
        this.orderService = new OrderService(orders, promotions, pricing, events, dispatch, riders, clock);
        this.catalog = new CatalogService(restaurants, promotions, orders);
        this.reports = new ReportService(orders, restaurants, customers, riders, clock);
    }

    /** In-memory platform for tests. */
    public static Platform forTesting(MutableClock clock) { return new Platform(PlatformConfig.defaults(), clock, null); }

    public PlatformConfig config()                         { return config; }
    public MutableClock clock()                            { return clock; }
    public InMemoryRepository<Restaurant> restaurants()    { return restaurants; }
    public InMemoryRepository<Customer> customers()        { return customers; }
    public InMemoryRepository<Rider> riders()              { return riders; }
    public InMemoryRepository<Order> orders()              { return orders; }
    public OrderEventBus events()                          { return events; }
    public CustomerNotifier notifier()                     { return notifier; }
    public RiderDashboard riderDashboard()                 { return riderDashboard; }
    public AuditLog auditLog()                             { return auditLog; }
    public PlatformStatistics statistics()                 { return statistics; }
    public MenuItemFactory menuItemFactory()               { return menuItemFactory; }
    public VehicleRegistry vehicles()                      { return vehicles; }
    public PricingEngine pricing()                         { return pricing; }
    public DispatchService dispatch()                      { return dispatch; }
    public OrderService orderService()                     { return orderService; }
    public CatalogService catalog()                        { return catalog; }
    public ReportService reports()                         { return reports; }
}
