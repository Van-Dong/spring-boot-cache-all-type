package com.dongnv.democache.config;

import java.util.Set;

public final class CacheNames {

    private CacheNames() {
    }

    public static final class Caffeine {
        public static final String USER = "USER_CACHE";
        public static final String PRODUCT = "PRODUCT_CACHE";
        public static final String ORDER = "ORDER_CACHE";
        public static final String CONFIG = "CONFIG_CACHE";
        public static final String SESSION = "SESSION_CACHE";
        public static final String CATEGORY = "CATEGORY_CACHE";
        public static final String DICTIONARY = "DICTIONARY_CACHE";

        public static final Set<String> ALL = Set.of(USER, PRODUCT, ORDER, CONFIG, SESSION, CATEGORY, DICTIONARY);

        private Caffeine() {
        }
    }

    public static final class Redis {
        public static final String USERS = "users";
        public static final String AUTH = "auth";
        public static final String PRODUCTS = "products";
        public static final String ORDERS = "orders";
        public static final String CONFIGS = "configs";

        public static final Set<String> ALL = Set.of(USERS, AUTH, PRODUCTS, ORDERS, CONFIGS);

        private Redis() {
        }
    }
}
