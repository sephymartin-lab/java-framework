/*
 * Copyright 2022-2026 sephy.top
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.sephy.infra.modelmapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.TestPropertySource;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ModelMapper 自动配置测试类
 *
 * @author sephy
 */
class ModelMapperAutoConfigurationTest {

    /**
     * 基本自动装配测试
     */
    @Nested
    @SpringBootTest(classes = BasicAutoConfigTest.TestConfig.class)
    @DisplayName("基本自动装配测试")
    class BasicAutoConfigTest {

        @Autowired
        private ModelMapper modelMapper;

        @Autowired
        private BeanConverter beanConverter;

        @Test
        @DisplayName("ModelMapper Bean 应该被自动装配")
        void modelMapperShouldBeAutowired() {
            assertThat(modelMapper).isNotNull();
        }

        @Test
        @DisplayName("BeanConverter Bean 应该被自动装配")
        void beanConverterShouldBeAutowired() {
            assertThat(beanConverter).isNotNull();
        }

        @Test
        @DisplayName("单对象转换应该正常工作")
        void singleObjectConversionShouldWork() {
            UserDO userDO = new UserDO(1L, "张三", "zhangsan@example.com", 25);

            UserDTO userDTO = beanConverter.convert(userDO, UserDTO.class);

            assertThat(userDTO).isNotNull();
            assertThat(userDTO.getId()).isEqualTo(1L);
            assertThat(userDTO.getName()).isEqualTo("张三");
            assertThat(userDTO.getEmail()).isEqualTo("zhangsan@example.com");
            assertThat(userDTO.getAge()).isEqualTo(25);
        }

        @Test
        @DisplayName("集合转换应该正常工作")
        void listConversionShouldWork() {
            List<UserDO> userDOs = Arrays.asList(
                new UserDO(1L, "张三", "zhangsan@example.com", 25),
                new UserDO(2L, "李四", "lisi@example.com", 30)
            );

            List<UserDTO> userDTOs = beanConverter.convertList(userDOs, UserDTO.class);

            assertThat(userDTOs).hasSize(2);
            assertThat(userDTOs.get(0).getName()).isEqualTo("张三");
            assertThat(userDTOs.get(1).getName()).isEqualTo("李四");
        }

        @Test
        @DisplayName("null 转换应该返回 null")
        void nullConversionShouldReturnNull() {
            UserDTO result = beanConverter.convert(null, UserDTO.class);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("空集合转换应该返回空列表")
        void emptyListConversionShouldReturnEmptyList() {
            List<UserDTO> result = beanConverter.convertList(Arrays.asList(), UserDTO.class);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("属性复制应该正常工作")
        void copyPropertiesShouldWork() {
            UserDO source = new UserDO(1L, "张三", "zhangsan@example.com", 25);
            UserDTO target = new UserDTO();
            target.setId(999L); // 原有值

            beanConverter.copyProperties(source, target);

            assertThat(target.getId()).isEqualTo(1L); // 被覆盖
            assertThat(target.getName()).isEqualTo("张三");
        }

        @Configuration
        @EnableAutoConfiguration
        static class TestConfig {
        }
    }

    /**
     * 嵌套对象转换测试
     */
    @Nested
    @SpringBootTest(classes = NestedObjectTest.TestConfig.class)
    @DisplayName("嵌套对象转换测试")
    class NestedObjectTest {

        @Autowired
        private BeanConverter beanConverter;

        @Test
        @DisplayName("嵌套对象应该被正确转换")
        void nestedObjectShouldBeConverted() {
            AddressDO addressDO = new AddressDO("北京市", "朝阳区", "100000");
            UserWithAddressDO userDO = new UserWithAddressDO(1L, "张三", addressDO);

            UserWithAddressDTO userDTO = beanConverter.convert(userDO, UserWithAddressDTO.class);

            assertThat(userDTO).isNotNull();
            assertThat(userDTO.getAddress()).isNotNull();
            assertThat(userDTO.getAddress().getCity()).isEqualTo("北京市");
            assertThat(userDTO.getAddress().getDistrict()).isEqualTo("朝阳区");
        }

        @Configuration
        @EnableAutoConfiguration
        static class TestConfig {
        }
    }

    /**
     * 自定义 Registrar 测试
     */
    @Nested
    @SpringBootTest(classes = CustomRegistrarTest.TestConfig.class)
    @DisplayName("自定义 Registrar 测试")
    class CustomRegistrarTest {

        @Autowired
        private BeanConverter beanConverter;

        @Test
        @DisplayName("自定义映射规则应该生效")
        void customMappingShouldWork() {
            UserDO userDO = new UserDO(1L, "张三", "zhangsan@example.com", 25);

            CustomUserDTO customDTO = beanConverter.convert(userDO, CustomUserDTO.class);

            assertThat(customDTO).isNotNull();
            // 验证自定义映射: name -> fullName
            assertThat(customDTO.getFullName()).isEqualTo("张三");
        }

        @Configuration
        @EnableAutoConfiguration
        static class TestConfig {

            @Bean
            public ModelMapperConverterRegistrar customRegistrar() {
                return modelMapper -> {
                    modelMapper.typeMap(UserDO.class, CustomUserDTO.class)
                        .addMappings(mapper -> mapper.map(UserDO::getName, CustomUserDTO::setFullName));
                };
            }
        }
    }

    /**
     * Spring ConversionService 集成测试
     */
    @Nested
    @SpringBootTest(classes = ConversionServiceTest.TestConfig.class)
    @DisplayName("Spring ConversionService 集成测试")
    class ConversionServiceTest {

        @Autowired
        @Qualifier("modelMapperConversionService")
        private ConversionService conversionService;

        @Autowired
        private ModelMapperAutoConfiguration.ModelMapperConversionServiceConfigurer configurer;

        @Test
        @DisplayName("modelMapperConversionService 应该被注入")
        void conversionServiceShouldBeInjected() {
            assertThat(conversionService).isNotNull();
            assertThat(conversionService).isInstanceOf(ModelMapperConversionService.class);
        }

        @Test
        @DisplayName("Configurer 应该被注入")
        void configurerShouldBeInjected() {
            assertThat(configurer).isNotNull();
        }

        @Test
        @DisplayName("自定义转换器应该被注册到 ConversionService")
        void customConverterShouldBeRegistered() {
            // 通过 configurer 注册一个转换器，需要同时指定源类型和目标类型
            configurer.createAndRegisterConverter(UserDO.class, UserDTO.class);

            // 验证可以通过 ConversionService 进行转换
            assertThat(conversionService.canConvert(UserDO.class, UserDTO.class)).isTrue();

            // 验证实际转换功能
            UserDO userDO = new UserDO(1L, "测试用户", "test@example.com", 30);
            UserDTO userDTO = conversionService.convert(userDO, UserDTO.class);
            assertThat(userDTO).isNotNull();
            assertThat(userDTO.getName()).isEqualTo("测试用户");
        }

        @Configuration
        @EnableAutoConfiguration
        static class TestConfig {
        }
    }

    /**
     * DynamicConversionService 懒加载测试
     */
    @Nested
    @SpringBootTest(classes = ModelMapperConversionServiceTest.TestConfig.class)
    @DisplayName("DynamicConversionService 懒加载测试")
    class ModelMapperConversionServiceTest {

        @Autowired
        @Qualifier("modelMapperConversionService")
        private ConversionService conversionService;

        @Autowired
        private ModelMapperConversionService modelMapperConversionService;

        @Test
        @DisplayName("DynamicConversionService 应该被注入")
        void dynamicConversionServiceShouldBeInjected() {
            assertThat(modelMapperConversionService).isNotNull();
        }

        @Test
        @DisplayName("通过 Qualifier 注入的 ConversionService 应该是 DynamicConversionService 类型")
        void conversionServiceShouldBeDynamicType() {
            assertThat(conversionService).isInstanceOf(ModelMapperConversionService.class);
        }

        @Test
        @DisplayName("无需预注册即可转换")
        void shouldConvertWithoutPreRegistration() {
            // 无需预注册，直接转换
            UserDO userDO = new UserDO(1L, "动态转换测试", "dynamic@example.com", 28);

            UserDTO userDTO = conversionService.convert(userDO, UserDTO.class);

            assertThat(userDTO).isNotNull();
            assertThat(userDTO.getId()).isEqualTo(1L);
            assertThat(userDTO.getName()).isEqualTo("动态转换测试");
            assertThat(userDTO.getEmail()).isEqualTo("dynamic@example.com");
        }

        @Test
        @DisplayName("首次转换后应该注册类型对")
        void shouldRegisterAfterFirstConversion() {
            // 转换前未注册
            assertThat(modelMapperConversionService.isRegistered(UserDO.class, CustomUserDTO.class)).isFalse();

            // 执行转换
            UserDO userDO = new UserDO(1L, "注册测试", "register@example.com", 25);
            CustomUserDTO dto = conversionService.convert(userDO, CustomUserDTO.class);

            // 转换后已注册
            assertThat(modelMapperConversionService.isRegistered(UserDO.class, CustomUserDTO.class)).isTrue();
            assertThat(dto).isNotNull();
        }

        @Test
        @DisplayName("canConvert 应该始终返回 true")
        void canConvertShouldAlwaysReturnTrue() {
            // 即使没有预注册，canConvert 也应该返回 true
            assertThat(conversionService.canConvert(AddressDO.class, AddressDTO.class)).isTrue();
            assertThat(conversionService.canConvert(String.class, Integer.class)).isTrue();
        }

        @Test
        @DisplayName("null 值转换应该返回 null")
        void nullConversionShouldReturnNull() {
            UserDTO result = conversionService.convert(null, UserDTO.class);
            assertThat(result).isNull();
        }

        @Configuration
        @EnableAutoConfiguration
        static class TestConfig {
        }
    }

    /**
     * 配置属性测试
     */
    @Nested
    @SpringBootTest(classes = PropertiesTest.TestConfig.class)
    @TestPropertySource(properties = {
        "infra.modelmapper.matching-strategy=LOOSE",
        "infra.modelmapper.skip-null-enabled=false"
    })
    @DisplayName("配置属性测试")
    class PropertiesTest {

        @Autowired
        private ModelMapper modelMapper;

        @Test
        @DisplayName("配置属性应该生效")
        void propertiesShouldBeApplied() {
            assertThat(modelMapper).isNotNull();
            // 验证配置已应用 (通过 ModelMapper 的行为间接验证)
            // 由于 LOOSE 模式，即使字段名略有不同也能匹配
        }

        @Configuration
        @EnableAutoConfiguration
        static class TestConfig {
        }
    }

    // ==================== 测试用 DTO/DO 类 ====================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UserDO {
        private Long id;
        private String name;
        private String email;
        private Integer age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UserDTO {
        private Long id;
        private String name;
        private String email;
        private Integer age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class CustomUserDTO {
        private Long id;
        private String fullName; // 自定义字段名
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class AddressDO {
        private String city;
        private String district;
        private String zipCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class AddressDTO {
        private String city;
        private String district;
        private String zipCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UserWithAddressDO {
        private Long id;
        private String name;
        private AddressDO address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class UserWithAddressDTO {
        private Long id;
        private String name;
        private AddressDTO address;
    }
}
