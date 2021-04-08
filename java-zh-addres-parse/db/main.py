import records
import json

db = records.Database('sqlite:///data.db')
drop_table_sql = "drop table if exists area"
create_table_sql = """create table area (
    code varchar(20) PRIMARY KEY,
    name varchar(100),
    level TINYINT(1),
    parent_code varchar(20)
)"""
index_sql = """
CREATE INDEX idx_area_name ON area (name,level);
"""
db.query(drop_table_sql)
db.query(create_table_sql)
db.query(index_sql)

# province
with open('./area/provinces.json') as json_file:
    data = json.load(json_file)
    sql = """
      insert into area(code,name,level) values(:code,:name,1)
    """
    db.bulk_query(sql, data)

# city
with open('./area/cities.json') as json_file:
    data = json.load(json_file)
    sql = """
      insert into area(code,name,level,parent_code) values(:code,:name,2,:provinceCode)
    """
    db.bulk_query(sql, data)

# area
with open('./area/areas.json') as json_file:
    data = json.load(json_file)
    sql = """
      insert into area(code,name,level,parent_code) values(:code,:name,3,:cityCode)
    """
    db.bulk_query(sql, data)

# street
with open('./area/streets.json') as json_file:
    data = json.load(json_file)
    sql = """
      insert into area(code,name,level,parent_code) values(:code,:name,4,:areaCode)
    """
    db.bulk_query(sql, data)
