package demo;

import static demo.inventory.InventoryStatus.IN_STOCK;

import demo.address.Address;
import demo.address.AddressRepository;
import demo.catalog.Catalog;
import demo.catalog.CatalogRepository;
import demo.inventory.Inventory;
import demo.inventory.InventoryRepository;
import demo.product.Product;
import demo.product.ProductRepository;
import demo.shipment.Shipment;
import demo.shipment.ShipmentRepository;
import demo.shipment.ShipmentStatus;
import demo.warehouse.Warehouse;
import demo.warehouse.WarehouseRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = InventoryApplication.class)
public class InventoryApplicationTests {

    @Autowired
    private ProductRepository productsRepo;

    @Autowired
    private ShipmentRepository shipmentsRepo;

    @Autowired
    private WarehouseRepository warehousesRepo;

    @Autowired
    private AddressRepository addressesRepo;

    @Autowired
    private CatalogRepository catalogsRepo;

    @Autowired
    private InventoryRepository inventoriesRepo;

    @Autowired
    private Session session;

    @Before
    public void setup() {
        try {
            session.query("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n, r;", Collections.emptyMap())
                    .queryResults();
        } catch (Exception e) {
            Assert.fail("can't connect to Neo4j! " + ExceptionUtils.getMessage(e));
        }
    }

    @Test
    public void testSave() {
        Product saved = productsRepo.save(new Product("Best. Cloud. Ever. (T-Shirt, Men's Large)", "SKU-24642", 21.99));
        Assert.assertNotNull(saved);
    }

    @Test
    public void inventoryTest() {
        // <1>
        List<Product> products = Stream
                .of(new Product("Best. Cloud. Ever. (T-Shirt, Men's Large)", "SKU-24642", 21.99),
                        new Product("Like a BOSH (T-Shirt, Women's Medium)", "SKU-34563", 14.99),
                        new Product("We're gonna need a bigger VM (T-Shirt, Women's Small)", "SKU-12464", 13.99),
                        new Product("cf push awesome (Hoodie, Men's Medium)", "SKU-64233", 21.99))
                .map(p -> productsRepo.save(p))
                .collect(Collectors.toList());

        Product sample = products.get(0);
        Assert.assertEquals(sample.getUnitPrice(), productsRepo.findById(sample.getId()).orElse(new Product()).getUnitPrice());

        // <2>
        catalogsRepo.save(new Catalog("Spring Catalog", products));

        // <3>
        Address warehouseAddress = addressesRepo.save(new Address("875 Howard St",
                null, "CA", "San Francisco", "United States", 94103));
        Address shipToAddress = addressesRepo.save(new Address(
                "1600 Amphitheatre Parkway", null, "CA", "Mountain View", "United States",
                94043));

        // <4>
        Warehouse warehouse = warehousesRepo.save(new Warehouse("Pivotal SF", warehouseAddress));
        Set<Inventory> inventories = products.stream()
                .map(p -> inventoriesRepo.save(new Inventory(UUID.randomUUID().toString(), p, warehouse, IN_STOCK)))
                .collect(Collectors.toSet());
        Shipment shipment = shipmentsRepo.save(new Shipment(inventories, shipToAddress, warehouse, ShipmentStatus.SHIPPED));
        Assert.assertEquals(inventories.size(), shipment.getInventories().size());
    }
}