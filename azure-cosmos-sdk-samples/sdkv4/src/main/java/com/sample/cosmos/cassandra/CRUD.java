package com.sample.cosmos.cassandra;

import com.datastax.driver.core.*;
import com.sample.cosmos.vo.UserAudienceInfoCass;

import java.util.List;

public class CRUD {
    private Session session;

    public static void main(String[] args) {
        CRUD crud = new CRUD();
        // Create a new item
        UserAudienceInfoCass useraudience = new UserAudienceInfoCass();
        useraudience.setUserId(700000001);
        for (int count=1; count <=100; count++) {
            useraudience.getAudiences().add(count);
        }
        //
        crud.init();
        //crud.selectuseraudience();
        for(int i=1; i<=1;i++) {
            System.out.println(i);
            crud.insertuseraudience(useraudience);
        }
        crud.selectuseraudience();
        //crud.deleteuseraudience();
        System.exit(0);
    }

    public void init() {
        //
        CassandraUtils utils = new CassandraUtils();
        session = utils.getSession();
    }

    public void insertuseraudience(final UserAudienceInfoCass useraudienceAudienceInfo) {
        //
        final String insertStatement = "UPDATE adtech.user2 set audience = audience + ? where userid = ?";
        BoundStatement boundStatement = new BoundStatement(session.prepare(insertStatement));
        ResultSet resultSet = session.execute(boundStatement.bind(
                useraudienceAudienceInfo.getAudiences(), 700000001L));
        Double requestCharge = resultSet.getExecutionInfo().getIncomingPayload().get("RequestCharge").getDouble();
        System.out.println("INSERT "+requestCharge);
    }

    public void selectuseraudience() {

        final String query = "SELECT * FROM adtech.user2 where userid = 700000001";
        ResultSet resultSet = session.execute(new SimpleStatement(query).setReadTimeoutMillis(100000));
        Double requestCharge = resultSet.getExecutionInfo().getIncomingPayload().get("RequestCharge").getDouble();
        System.out.println("SELECT "+requestCharge);
        for (Row row : resultSet.all()) {
            System.out.println("audiences size "+((List<Integer> )row.getObject("audience")).size());
            System.out.println("audiences info "+row.getObject("audience"));
        }
    }

    public void deleteuseraudience() {

        final String query = "delete FROM adtech.user2 where userid = 700000001";
        ResultSet resultSet = session.execute(query);
        Double requestCharge = resultSet.getExecutionInfo().getIncomingPayload().get("RequestCharge").getDouble();
        System.out.println("Delete "+requestCharge);
    }
}
