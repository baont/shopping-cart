package com.shashi.service.impl;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.shashi.beans.CartBean;
import com.shashi.beans.OrderBean;
import com.shashi.beans.TransactionBean;

public class OrderServiceImplTest {

	@Test
	public void paymentSuccess_shouldMultiplyLineAmountBy10_andReturnSuccess() throws Exception {
		String userName = "user@test.com";
		double paidAmount = 10.0;

		CartBean item = new CartBean();
		setField(item, "userId", userName);
		setField(item, "prodId", "P1");
		setField(item, "quantity", 2);

		List<CartBean> cartItems = new ArrayList<CartBean>();
		cartItems.add(item);

		OrderServiceImpl spyService = Mockito.spy(new OrderServiceImpl());
		Mockito.doReturn(true).when(spyService).addOrder(Mockito.any(OrderBean.class));
		Mockito.doReturn(true).when(spyService).addTransaction(Mockito.any(TransactionBean.class));

  try (org.mockito.MockedStatic<com.shashi.utility.MailMessage> mailMessageMock = Mockito
    .mockStatic(com.shashi.utility.MailMessage.class);
   MockedConstruction<CartServiceImpl> cartMock = Mockito.mockConstruction(CartServiceImpl.class,
     (mock, context) -> {
      Mockito.when(mock.getAllCartItems(userName)).thenReturn(cartItems);
      Mockito.when(mock.removeAProduct(userName, "P1")).thenReturn(true);
     });
				MockedConstruction<ProductServiceImpl> prodMock = Mockito.mockConstruction(ProductServiceImpl.class,
						(mock, context) -> {
							Mockito.when(mock.getProductPrice("P1")).thenReturn(5.0);
							Mockito.when(mock.sellNProduct("P1", 2)).thenReturn(true);
						});
				MockedConstruction<OrderServiceImpl> orderServiceMock = Mockito.mockConstruction(OrderServiceImpl.class,
						(mock, context) -> Mockito.when(mock.addTransaction(Mockito.any(TransactionBean.class)))
								.thenReturn(true));
				MockedConstruction<UserServiceImpl> userServiceMock = Mockito.mockConstruction(UserServiceImpl.class,
						(mock, context) -> Mockito.when(mock.getFName(userName)).thenReturn("Test"))) {

   mailMessageMock
     .when(() -> com.shashi.utility.MailMessage.transactionSuccess(Mockito.eq(userName), Mockito.anyString(),
       Mockito.anyString(), Mockito.anyDouble()))
     .thenAnswer(invocation -> null);

			String status = spyService.paymentSuccess(userName, paidAmount);

			assertEquals("Order Placed Successfully!", status);

			Mockito.verify(spyService).addOrder(Mockito.argThat(order -> {
				assertEquals("P1", order.getProductId());
				assertEquals(2, order.getQuantity());
				assertEquals(5.0 * 2 * 10, order.getAmount(), 0.0001);
				return true;
			}));

			mailMessageMock.verify(() -> com.shashi.utility.MailMessage.transactionSuccess(Mockito.eq(userName),
					Mockito.anyString(), Mockito.anyString(), Mockito.anyDouble()));
		}
	}

	@Test
	public void paymentSuccess_whenCartEmpty_shouldFail() {
		String userName = "user@test.com";
		double paidAmount = 10.0;

		OrderServiceImpl service = Mockito.spy(new OrderServiceImpl());

		try (MockedConstruction<CartServiceImpl> cartMock = Mockito.mockConstruction(CartServiceImpl.class,
				(mock, context) -> Mockito.when(mock.getAllCartItems(userName)).thenReturn(new ArrayList<CartBean>()))) {

			String status = service.paymentSuccess(userName, paidAmount);
			assertEquals("Order Placement Failed!", status);
			Mockito.verify(service, Mockito.never()).addOrder(Mockito.any(OrderBean.class));
		}
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field f = null;
		Class<?> c = target.getClass();
		while (c != null) {
			try {
				f = c.getDeclaredField(fieldName);
				break;
			} catch (NoSuchFieldException ex) {
				c = c.getSuperclass();
			}
		}
		if (f == null) {
			throw new NoSuchFieldException(fieldName);
		}
		f.setAccessible(true);
		f.set(target, value);
	}
}
