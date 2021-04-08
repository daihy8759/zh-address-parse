package com.github.daihy8759.util.db;

import com.github.daihy8759.util.model.Area;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

public class SqliteUtil {

  private SqliteUtil() {
  }

  public static HikariDataSource dataSource = new HikariDataSource();

  static {
    dataSource.setDriverClassName("org.sqlite.JDBC");
    dataSource.setJdbcUrl("jdbc:sqlite::resource:data.db");
  }

  public static DataSource getDataSource() {
    return dataSource;
  }

  @SneakyThrows
  public static List<Area> getArea(String parentCode, String nameLike, int level) {
    if (parentCode == null || "".equals(parentCode)) {
      return getArea(nameLike, level);
    }
    QueryRunner qr = new QueryRunner(getDataSource());
    return qr
        .query("select code,name,parent_code as parentCode from area where level = ? and parentCode=? and name like ?",
            new BeanListHandler<>(Area.class),
            level, parentCode, nameLike + "%");
  }

  @SneakyThrows
  private static List<Area> getArea(String nameLike, int level) {
    QueryRunner qr = new QueryRunner(getDataSource());
    return qr
        .query("select code,name,parent_code as parentCode from area where level = ? and name like ?",
            new BeanListHandler<>(Area.class), level, nameLike + "%");
  }

  @SneakyThrows
  public static Area getAreaSingle(String code, int level) {
    QueryRunner qr = new QueryRunner(getDataSource());
    return qr
        .query("select code,name,parent_code as parentCode from area where level = ? and code = ?",
            new BeanHandler<>(Area.class), level, code);
  }

}
