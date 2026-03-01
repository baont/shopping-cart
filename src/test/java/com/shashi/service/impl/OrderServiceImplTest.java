package com.shashi.service.impl;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import com.shashi.beans.CartBean;
import com.shashi.beans.OrderBean;
import com.shashi.beans.TransactionBean;
import com.shashi.utility.MailMessage;

import static org.mockito.Mockito.*;

public class OrderServiceImplTest {

	@Test
	public void paymentSuccess_shouldMultiplyLineAmountBy10_andReturnSuccess() {
		String userName = "user@example.com";
		double paidAmount = 123.45;
		String prodId = "P123";
		int qty = 2;
		double unitPrice = 50.0;
		double expectedAmount = unitPrice * qty * 10;

		CartBean cartItem = new CartBean();
		setField(cartItem, "userId", userName);
		setField(cartItem, "prodId", prodId);
		setField(cartItem, "quantity", qty);

		try (MockedConstruction<CartServiceImpl> cartMocked = mockConstruction(CartServiceImpl.class,
				(mock, context) -> {
					when(mock.getAllCartItems(userName)).thenReturn(Arrays.asList(cartItem));
					when(mock.removeAProduct(userName, prodId)).thenReturn(true);
				});
				MockedConstruction<ProductServiceImpl> productMocked = mockConstruction(ProductServiceImpl.class,
						(mock, context) -> {
							when(mock.getProductPrice(prodId)).thenReturn(unitPrice);
							when(mock.sellNProduct(prodId, qty)).thenReturn(true);
						});
				MockedConstruction<UserServiceImpl> userMocked = mockConstruction(UserServiceImpl.class,
						(mock, context) -> when(mock.getFName(userName)).thenReturn("John"));
				MockedConstruction<OrderServiceImpl> orderMocked = mockConstruction(OrderServiceImpl.class,
						(mock, context) -> {
							when(mock.addTransaction(any(TransactionBean.class))).thenReturn(true);
						});
				MockedStatic<MailMessage> mailStatic = mockStatic(MailMessage.class)) {

			OrderServiceImpl service = new OrderServiceImpl();
			OrderServiceImpl serviceSpy = spy(service);
			doReturn(true).when(serviceSpy).addOrder(any(OrderBean.class));

			String status = serviceSpy.paymentSuccess(userName, paidAmount);
			assertEquals("Order Placed Successfully!", status);

			verify(serviceSpy).addOrder(argThat(order -> expectedAmount == order.getAmount()));
			mailStatic.verify(() -> MailMessage.transactionSuccess(eq(userName), eq("John"), anyString(), eq(paidAmount)));
		}
	}

	@Test
	public void paymentSuccess_whenCartEmpty_shouldFail() {
		String userName = "user@example.com";

		try (MockedConstruction<CartServiceImpl> cartMocked = mockConstruction(CartServiceImpl.class,
				(mock, context) -> when(mock.getAllCartItems(userName)).thenReturn(Collections.emptyList()));) {
			OrderServiceImpl service = new OrderServiceImpl();
			assertEquals("Order Placement Failed!", service.paymentSuccess(userName, 10.0));
		}
	}

	private static void setField(Object target, String fieldName, Object value) {
		try {
			Field f = target.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			f.set(target, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
