package org.productshopexer.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.productshopexer.data.entities.User;
import org.productshopexer.data.repositories.UserRepository;
import org.productshopexer.service.UserService;
import org.productshopexer.service.dtos.export.*;
import org.productshopexer.service.dtos.imports.UserSeedDto;
import org.productshopexer.util.ValidationUtil;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final String FILE_PATH = "src/main/resources/json/users.json";

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final Gson gson;

    public UserServiceImpl(UserRepository userRepository, ModelMapper modelMapper, ValidationUtil validationUtil, Gson gson) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.gson = gson;
    }

    @Override
    public void seedUsers() throws FileNotFoundException {
        if (this.userRepository.count() == 0) {
            UserSeedDto[] userSeedDtos = this.gson.fromJson(new FileReader(FILE_PATH), UserSeedDto[].class);
            for (UserSeedDto userSeedDto : userSeedDtos) {
                if (!this.validationUtil.isValid(userSeedDto)) {
                    this.validationUtil.getViolations(userSeedDto)
                            .forEach(v -> System.out.println(v.getMessage()));

                    continue;
                }

                this.userRepository.saveAndFlush(this.modelMapper.map(userSeedDto, User.class));
            }
        }
    }

    @Override
    public List<UserSoldProductsDto> getAllUsersAndSoldItems() {
        return this.userRepository.findAll()
                .stream()
                .filter(u ->
                        u.getSold().stream().anyMatch(p -> p.getBuyer() != null))
                .map(u -> {
                    UserSoldProductsDto userDto = this.modelMapper.map(u, UserSoldProductsDto.class);
                    List<ProductSoldDto> soldProductsDto = u.getSold()
                            .stream()
                            .filter(p -> p.getBuyer() != null)
                            .map(p -> this.modelMapper.map(p, ProductSoldDto.class))
                            .collect(Collectors.toList());
                    userDto.setSoldProducts(soldProductsDto);
                    return userDto;
                })
                .sorted(Comparator.comparing(UserSoldProductsDto::getLastName).thenComparing(UserSoldProductsDto::getFirstName))
                .toList();
    }

    @Override
    public void printAllUsersAndSoldItems() {
        String json = this.gson.toJson(this.getAllUsersAndSoldItems());
        System.out.println(json);
    }

    @Override
    public UserAndProductDto getUserAndProductDto() {
        UserAndProductDto userAndProductDto = new UserAndProductDto();
        List<UserSoldDto> userSoldDtos = this.userRepository.findAll()
                .stream()
                .filter(u -> !u.getSold().isEmpty())
                .map(u -> {
                    UserSoldDto userSoldDto = this.modelMapper.map(u, UserSoldDto.class);
                    ProductSoldByUserDto productSoldByUserDto = new ProductSoldByUserDto();

                    List<ProductInfoDto> productInfoDtos = u.getSold()
                            .stream()
                            .map(p -> this.modelMapper.map(p, ProductInfoDto.class))
                            .collect(Collectors.toList());
                    productSoldByUserDto.setProducts(productInfoDtos);
                    productSoldByUserDto.setCount(productInfoDtos.size());

                    userSoldDto.setSoldProducts(productSoldByUserDto);
                    return userSoldDto;
                })
                .sorted((a, b) -> {
                    int countA = a.getSoldProducts().getCount();
                    int countB = b.getSoldProducts().getCount();
                    return Integer.compare(countB,countA);
                })
                .collect(Collectors.toList());

        userAndProductDto.setUsers(userSoldDtos);
        userAndProductDto.setUsersCount(userSoldDtos.size());
        return userAndProductDto;
    }

    @Override
    public void printGetUserAndProductDto() {
        String json = this.gson.toJson(this.getUserAndProductDto());
        System.out.println(json);
    }
}
