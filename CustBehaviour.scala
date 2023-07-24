package com.customer_behaviour

import org.apache.spark.sql.{SparkSession, functions}
import org.apache.spark.sql.functions.{col, date_format, lit, split, to_date}

object CustBehaviour extends Serializable{
  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .appName("ecommerce")
      .master("local[3]")
      .config("spark.cassandra.connection.host", "localhost")
      .config("spark.cassandra.connection.port", "9042")
      .config("spark.sql.extensions", "com.datastax.spark.connector.CassandraSparkExtensions")
      .config("spark.sql.catalog.lh", "com.datastax.spark.connector.datasource.CassandraCatalog")
      .getOrCreate()
    var customerdf = spark.read.option("header", true).csv("Inputdata/customer_data.csv")
    var purchasedf = spark.read.option("header", true).csv("Inputdata/purchase_data.csv")
    var clickstreamdf = spark.read.option("header", true).csv("Inputdata/clickstream_data.csv")
    //customerdf.show()
    //purchase.show()
    //clickstream.show()
    val customer = customerdf.select(functions.split(col("userID,name,email"), ",").getItem(0).as("userID"),
      functions.split(col("userID,name,email"), ",").getItem(1).as("name"),
      functions.split(col("userID,name,email"), ",").getItem(2).as("email"))
    //customer.show()
    val purchase = purchasedf.select(functions.split(col("userID,timestamp,amount"), ",").getItem(0).as("userID"),
      functions.split(col("userID,timestamp,amount"), ",").getItem(1).as("timestamp"),
      functions.split(col("userID,timestamp,amount"), ",").getItem(2).as("amount"))
    //purchase.show()
    val click = clickstreamdf.select(functions.split(col("userID,timestamp,page"), ",").getItem(0).as("userID"),
      functions.split(col("userID,timestamp,page"), ",").getItem(1).as("timestamp"),
      functions.split(col("userID,timestamp,page"), ",").getItem(2).as("page"))
    click.show()
    customer.createOrReplaceTempView("customer1")
    purchase.createOrReplaceTempView("purchase1")
    val joinedDF = customer.join(purchase, Seq("userID"), joinType = "Inner")
    joinedDF.show()
    val purchase_df = purchase.withColumn("date", to_date(col("timestamp")))
    dfWithDate.show()
    val pur_2 = purchase_df.withColumn("time", date_format(col("timestamp"), "HH:mm:ss"))
    pur_2.show()
    val newpurchase = pur_2.drop("timestamp")
    newpurchase.show()
    val first_name = customer.withColumn("firstname", split(col("name"), " ").getItem(0))
    val last_name = first_name.withColumn("lastname", split(col("name"), " ").getItem(1))
    val newcustomer = last_name.withColumnRenamed("name","full_name")
    newcustomer.show()
    val click_df = click.withColumn("date", to_date(col("timestamp")))
    val click_2 = click_df.withColumn("time", date_format(col("timestamp"), "HH:mm:ss"))
    val newclick = click_2.drop("timestamp")
    newclick.show()
    val new_customer = newcustomer.withColumnRenamed("userID", "userid")
    val new_purchase = newpurchase.withColumnRenamed("userID", "userid")
    val new_click = newclick.withColumnRenamed("userID", "userid")
    new_customer.write.options(Map("header" -> "true", "delimiter" -> ",")).csv("Inputdata/newcustomer.csv")
    new_purchase.write.options(Map("header" -> "true", "delimiter" -> ",")).csv("Inputdata/newpurchase.csv")
    new_click.write.options(Map("header" -> "true", "delimiter" -> ",")).csv("Inputdata/newclick.csv")
    new_customer.write
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace", "prac")
      .option("table", "customer")
      .mode("append")
      .save()
    new_purchase.write
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace", "prac")
      .option("table", "purchase")
      .mode("append")
      .save()
    new_click.write
      .format("org.apache.spark.sql.cassandra")
      .option("keyspace", "prac")
      .option("table", "clickstream")
      .mode("append")
      .save()
  }
}
