package com.mv.schelokov.car_rent.model.services;

import com.mv.schelokov.car_rent.model.db.repository.exceptions.DbException;
import com.mv.schelokov.car_rent.model.db.repository.exceptions.RepositoryException;
import com.mv.schelokov.car_rent.model.db.repository.factories.CriteriaFactory;
import com.mv.schelokov.car_rent.model.db.repository.factories.RepositoryFactory;
import com.mv.schelokov.car_rent.model.db.repository.interfaces.Criteria;
import com.mv.schelokov.car_rent.model.db.repository.interfaces.Repository;
import com.mv.schelokov.car_rent.model.entities.RentOrder;
import com.mv.schelokov.car_rent.model.services.exceptions.ServiceException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * @author Maxim Chshelokov <schelokov.mv@gmail.com>
 */
public class OrderService {
    
    private static final Logger LOG = Logger.getLogger(OrderService.class);
    private static final String ORDER_REPOSITORY_ERROR = "Failed to get an order "
            + "list form the repository by the criteria";
    private static final String ORDER_OPERATION_ERROR = "Failed to write an order";
    
    private enum Operation {
        CREATE, UPDATE, DELETE
    }

    
    public List getAllOrders() throws ServiceException {
        Criteria criteria = CriteriaFactory.getAllOrdersOrderByApproved();
        List<RentOrder> orders = getOrdersByCriteria(criteria);
        for (RentOrder order : orders)
            calculateSum(order);
        return orders;
    }
    
    public RentOrder getOrderById(int id) throws ServiceException {
        Criteria criteria = CriteriaFactory.findOrderById(id);
        List resultList = getOrdersByCriteria(criteria);
        if (resultList.isEmpty())
            throw new ServiceException(
                    String.format("There is no rent order having id = %d", id));
        RentOrder result = (RentOrder) resultList.get(0);
        calculateSum(result);
        return result;
    }
    
    public void addOrder(RentOrder order) throws ServiceException {
        operateOrder(order, Operation.CREATE);
    }
    
    public void updateOrder(RentOrder order) throws ServiceException {
        operateOrder(order, Operation.UPDATE);
    }
    
    public void deleteOrder(RentOrder order) throws ServiceException {
        operateOrder(order, Operation.DELETE);
    }
    
    private void operateOrder(RentOrder order, Operation operation)
            throws ServiceException {
        try (RepositoryFactory repositoryFactory = new RepositoryFactory()) {
            Repository orderRepository = repositoryFactory
                    .getRentOrderRepository();
            switch (operation) {
                case CREATE:
                    orderRepository.add(order);
                    break;
                case UPDATE:
                    orderRepository.update(order);
                    break;
                case DELETE:
                    orderRepository.remove(order);
            }
        }
        catch (RepositoryException | DbException ex) {
            LOG.error(ORDER_OPERATION_ERROR, ex);
            throw new ServiceException(ORDER_OPERATION_ERROR, ex);
        } 
    }
    
    private void calculateSum(RentOrder order) {
        order.setSum(order.getCar().getPrice() * (int) TimeUnit.MILLISECONDS
                    .toDays(order.getEndDate().getTime()
                            - order.getStartDate().getTime()));
    }
    
    private List getOrdersByCriteria(Criteria criteria)
            throws ServiceException {
        try(RepositoryFactory repositoryFactory = new RepositoryFactory()) {
            Repository orderRepository = repositoryFactory
                    .getRentOrderRepository();
            return orderRepository.read(criteria);
        } catch (RepositoryException | DbException ex) {
            LOG.error(ORDER_REPOSITORY_ERROR, ex);
            throw new ServiceException(ORDER_REPOSITORY_ERROR, ex);
        }
    }
    
}
