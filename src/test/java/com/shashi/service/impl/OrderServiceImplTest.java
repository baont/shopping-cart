package com.shashi.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.shashi.beans.OrderBean;
import com.shashi.utility.DBUtil;

public class OrderServiceImplTest {

	@Test
	public void addOrder_success_returnsTrue_andSetsAmount() throws Exception {
		OrderBean order = new OrderBean("trans-1", "prod-1", 2, 123.45);

		Connection con = Mockito.mock(Connection.class);
		PreparedStatement ps = Mockito.mock(PreparedStatement.class);
		Mockito.when(con.prepareStatement(Mockito.anyString())).thenReturn(ps);
		Mockito.when(ps.executeUpdate()).thenReturn(1);

		try (MockedStatic<DBUtil> dbUtil = Mockito.mockStatic(DBUtil.class)) {
			dbUtil.when(DBUtil::provideConnection).thenReturn(con);

			boolean result = new OrderServiceImpl().addOrder(order);

			assertTrue(result);
			Mockito.verify(con).prepareStatement("insert into orders values(?,?,?,?,?)");
			Mockito.verify(ps).setString(1, "trans-1");
			Mockito.verify(ps).setString(2, "prod-1");
			Mockito.verify(ps).setInt(3, 2);
			Mockito.verify(ps).setDouble(4, 123.45);
			Mockito.verify(ps).setInt(5, 0);
			Mockito.verify(ps).executeUpdate();
		}
	}

	@Test
	public void addOrder_executeUpdateReturns0_returnsFalse() throws Exception {
		OrderBean order = new OrderBean("trans-1", "prod-1", 2, 123.45);

		Connection con = Mockito.mock(Connection.class);
		PreparedStatement ps = Mockito.mock(PreparedStatement.class);
		Mockito.when(con.prepareStatement(Mockito.anyString())).thenReturn(ps);
		Mockito.when(ps.executeUpdate()).thenReturn(0);

		try (MockedStatic<DBUtil> dbUtil = Mockito.mockStatic(DBUtil.class)) {
			dbUtil.when(DBUtil::provideConnection).thenReturn(con);

			boolean result = new OrderServiceImpl().addOrder(order);

			assertFalse(result);
		}
	}

	@Test
	public void countSoldItem_success_returnsSum_fromFirstColumn() throws Exception {
		Connection con = Mockito.mock(Connection.class);
		PreparedStatement ps = Mockito.mock(PreparedStatement.class);
		ResultSet rs = Mockito.mock(ResultSet.class);

		Mockito.when(con.prepareStatement(Mockito.anyString())).thenReturn(ps);
		Mockito.when(ps.executeQuery()).thenReturn(rs);
		Mockito.when(rs.next()).thenReturn(true);
		Mockito.when(rs.getInt(1)).thenReturn(7);

		try (MockedStatic<DBUtil> dbUtil = Mockito.mockStatic(DBUtil.class)) {
			dbUtil.when(DBUtil::provideConnection).thenReturn(con);

			int result = new OrderServiceImpl().countSoldItem("prod-1");

			assertEquals(7, result);
			Mockito.verify(con).prepareStatement("select sum(quantity) from orders where prodid=?");
			Mockito.verify(ps).setString(1, "prod-1");
			Mockito.verify(ps).executeQuery();
			dbUtil.verify(() -> DBUtil.closeConnection(con));
			dbUtil.verify(() -> DBUtil.closeConnection(ps));
			dbUtil.verify(() -> DBUtil.closeConnection(rs));
		}
	}

	@Test
	public void countSoldItem_whenSQLException_returns0_andClosesResources() throws Exception {
		Connection con = Mockito.mock(Connection.class);
		PreparedStatement ps = Mockito.mock(PreparedStatement.class);

		Mockito.when(con.prepareStatement(Mockito.anyString())).thenReturn(ps);
		Mockito.when(ps.executeQuery()).thenThrow(new SQLException("boom"));

		try (MockedStatic<DBUtil> dbUtil = Mockito.mockStatic(DBUtil.class)) {
			dbUtil.when(DBUtil::provideConnection).thenReturn(con);

			int result = new OrderServiceImpl().countSoldItem("prod-1");

			assertEquals(0, result);
			dbUtil.verify(() -> DBUtil.closeConnection(con));
			dbUtil.verify(() -> DBUtil.closeConnection(ps));
			dbUtil.verify(() -> DBUtil.closeConnection((ResultSet) null));
		}
	}
}
