package org.productshopexer.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.productshopexer.data.entities.Category;
import org.productshopexer.data.entities.Product;
import org.productshopexer.data.entities.User;
import org.productshopexer.data.repositories.CategoryRepository;
import org.productshopexer.data.repositories.ProductRepository;
import org.productshopexer.data.repositories.UserRepository;
import org.productshopexer.service.ProductService;
import org.productshopexer.service.dtos.export.ProductInRangeDto;
import org.productshopexer.service.dtos.imports.ProductSeedDto;
import org.productshopexer.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String FILE_PATH = "src/main/resources/json/products.json";

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;

    public ProductServiceImpl(ProductRepository productRepository, UserRepository userRepository, CategoryRepository categoryRepository, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
    }

    @Override
    public void seedProducts() throws FileNotFoundException {
        if (this.productRepository.count() == 0) {
            ProductSeedDto[] productSeedDtos = this.gson.fromJson(new FileReader(FILE_PATH), ProductSeedDto[].class);

            for (ProductSeedDto productSeedDto : productSeedDtos) {
                if (!this.validationUtil.isValid(productSeedDto)) {
                    this.validationUtil.getViolations(productSeedDto)
                            .forEach(v -> System.out.println(v.getMessage()));

                    continue;
                }

                Product product = this.modelMapper.map(productSeedDto, Product.class);
                product.setBuyer(getRandomUser(true));
                product.setSeller(getRandomUser(false));
                product.setCategories(getRandomCategories());

                this.productRepository.saveAndFlush(product);
            }
        }
    }

    @Override
    public List<ProductInRangeDto> getAllProductsInRange(BigDecimal from, BigDecimal to) {
        return this.productRepository.findAllByPriceBetweenAndBuyerIsNullOrderByPrice(from, to)
                .stream()
                .map(p -> {
                    ProductInRangeDto dto = this.modelMapper.map(p, ProductInRangeDto.class);
                    dto.setSeller(p.getSeller().getFirstName() + " " + p.getSeller().getLastName());
                    return dto;
                })
                .sorted(Comparator.comparing(ProductInRangeDto::getPrice))
                .collect(Collectors.toList());
    }

    @Override
    public void printAllProductsInRange(BigDecimal from, BigDecimal to) {
        System.out.println(this.gson.toJson(this.getAllProductsInRange(from, to)));
    }

    private Set<Category> getRandomCategories() {
        Set<Category> categories = new HashSet<>();

        int randomCount = ThreadLocalRandom.current().nextInt(1,4);
        for (int i = 0; i < randomCount ; i++) {
            long randomId = ThreadLocalRandom.current().nextLong(1, this.categoryRepository.count() + 1);
            categories.add(this.categoryRepository.findById(randomId).get());
        }

        return categories;
    }

    private User getRandomUser(boolean isBuyer) {
        long randomId = ThreadLocalRandom.current().nextLong(1, this.userRepository.count() + 1);

        return isBuyer && randomId % 4 == 0 ? null : this.userRepository.findById(randomId).get();
    }
}
