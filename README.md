# mini-fdu-socket

2024年春季学期《高级Web技术》课程实践项目 socket-api

# 开发指南

配置好数据库，用idea打开本项目进行开发即可。

1. 修改配置文件[application.properties](./src/main/resources/application.properties)，启用dev配置

    ```properties
    spring.profiles.active=dev
    ```

2. MySQL初始化：运行 `src/main/resources/init.sql` 文件

## 热部署

1. `pom.xml` 中添加依赖（已添加）

   ```xml
    <!--devtools热部署-->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <optional>true</optional>
        <scope>true</scope>
    </dependency>
    ```

2. 在配置文件中配置一下devtools（`application-dev.properties` 中已经添加）

    ```properties
    # devtools
    ## start the devtools
    spring.devtools.restart.enabled=true
    spring.devtools.restart.additional-paths=src/main/java
    spring.devtools.restart.exclude=static/**,public/**,resources/**,META-INF/**,templates/**,WEB-INF/**
    ```

3. 在idea中设置

    - （1）File-Settings-Compiler-Build Project automatically
    - （可能没有，可以忽略）（2）ctrl + shift + alt + / ,选择Registry,勾上 Compiler autoMake allow when app running

# docker 部署

## 后端docker镜像打包

> 不选择进行多阶段构建，而是本机构建 jar 包，因为多阶段构建更慢

1. 打包jar：maven-->MyTodo-->生命周期-->package （如果测试无法通过，可能需要使用`@Disabled`注解禁用测试）

2. 构建docker镜像

```shell
# 本地构建
docker build -t mini-fdu-socket:1.0.0 .
# 构建并推送到docker hub
## 如果需要推送到自己的docker hub，需要先登录，并更改 zmarkgo 为自己的用户名
docker build -t zmarkgo/mini-fdu-socket:1.0.0 .
docker push zmarkgo/mini-fdu-socket:1.0.0
```

## 使用docker部署后端

本地测试：

```shell
docker run -d --name mini-fdu-socket -p 8798:8798 -e SERVER_PORT=8798 -e MYSQL_HOST=host.docker.internal -e MYSQL_PORT=3306 -e MYSQL_USER_NAME=mini_fdu_admin -e MYSQL_USER_PASSWORD=password123 mini-fdu-socket:1.0.0 
```

```shell
# 单行命令
docker run -d --add-host=host.docker.internal:host-gateway --name mini-fdu-socket -p 8798:8798 -e SERVER_PORT=8798 -e MYSQL_HOST=host.docker.internal -e MYSQL_PORT=9004 -e MYSQL_USER_NAME=root -e MYSQL_USER_PASSWORD=root mini-fdu-socket:1.0.0 

# 多行命令
docker run -d \
--add-host=host.docker.internal:host-gateway \
--name mini-fdu-socket \
-p 8798:8798 \
-e SERVER_PORT=8798 \
-e MYSQL_HOST=host.docker.internal \
-e MYSQL_PORT=9004 \
-e MYSQL_USER_NAME=root \
-e MYSQL_USER_PASSWORD=root \
mini-fdu-socket:1.0.0 
```

- `-d` :后台运行容器，并返回容器ID
- `-add-host=host.docker.internal:host-gateway` : 添加一个自定义的主机到容器中，将容器内部的`host.docker.internal`
  解析为Docker宿主机的IP地址
- `--name` :指定容器名
- `-p` :指定端口映射
- `-e` :指定环境变量，环境变量配置参考下面的表格；
- `host.docker.internal`是Docker Desktop中的一个特殊域名，用于在容器内访问宿主机的地址
- 最后的`mini-fdu-socket:1.0.0`指定镜像

### 环境变量配置

在配置文件[application-prod.properties](./src/main/resources/application-prod.properties)中使用

| 变量名                   | 说明         | 默认值         |
|-----------------------|------------|-------------|
| `SERVER_PORT`         | 服务端口       | `8798`      |
| `MYSQL_HOST`          | MySQL 主机地址 | `localhost` |
| `MYSQL_PORT`          | MySQL 端口   | `9004`      |
| `MYSQL_USER_NAME`     | MySQL 用户名  | `root`      |
| `MYSQL_USER_PASSWORD` | MySQL 密码   | `root`      |
| `MYSQL_DATABASE`      | MySQL 数据库名 | `mini_fdu`  |