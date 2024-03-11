package org.productshopexer.service;

import org.productshopexer.service.dtos.export.UserAndProductDto;
import org.productshopexer.service.dtos.export.UserSoldProductsDto;

import java.io.FileNotFoundException;
import java.util.List;

public interface UserService {

    void seedUsers() throws FileNotFoundException;

    List<UserSoldProductsDto> getAllUsersAndSoldItems();

    void printAllUsersAndSoldItems();

    UserAndProductDto getUserAndProductDto();

    void printGetUserAndProductDto();
}
