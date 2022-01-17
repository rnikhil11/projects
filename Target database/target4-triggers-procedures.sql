-- Trigger to detect invalid order based on store inventory
CREATE or REPLACE TRIGGER Check_order_anomaly
BEFORE INSERT OR UPDATE OF item_count on order_items
FOR EACH ROW
DECLARE
    store_id varchar2(15);
    inventory NUMBER;
BEGIN
    select o.store_id into store_id from orders o where o.ORDER_ID=:new.ORDER_ID;
    select si.item_count into inventory from STORE_ITEMS si where si.STORE_ID=store_id and si.item_id=:new.item_id;
    IF inventory<:new.item_count THEN
        Raise_Application_Error(-20000, 'INSUFFICIENT INVENTORY of item '||:new.item_id);
    END IF;
END;

-- Trigger to check invalid insert or update of item deal
--  i.e. to maintain only one deal on an item at any point of time
CREATE or REPLACE TRIGGER Check_item_deal_validity
FOR INSERT OR UPDATE OF start_date, end_date on item_deal
COMPOUND TRIGGER
    
    type type_dates_list is table of date;
    type type_itemids_list is table of item.item_id%type;
    Item_IDs type_itemids_list;
    Start_dates type_dates_list;
    End_dates type_dates_list;

    before statement is
    BEGIN
    SELECT               id.item_id, id.start_date, id.end_date
      BULK COLLECT INTO  Item_IDs, Start_dates, End_dates
      FROM               ITEM_DEAL id;

    end before statement;


 AFTER EACH ROW IS
  BEGIN
    FOR j IN 1..Item_IDs.COUNT() LOOP
        if((:new.start_date<=Start_dates(j) and :new.end_date>=Start_dates(j)) or (:new.start_date<=End_dates(j) and :new.end_date>=End_dates(j))) THEN
                Raise_Application_Error(-20000, 'Overlap with deal');
                EXIT;
            end if;
    end loop;

END AFTER EACH ROW;
end Check_item_deal_validity;

        
-- stored Procedure to get the price of an item after deals and tax
create or replace PROCEDURE getItemPrice(itemId IN item.item_id%TYPE, itemCount IN number,  itemPrice OUT number) IS
itemDeal item_deal%ROWTYPE;
itemDetails ITEM%ROWTYPE;
curDate DATE;
CURSOR itemDeals is
SELECT id.*
FROM ITEM_DEAL id
WHERE id.ITEM_ID=itemId;

CURSOR itemDetailsArray is
SELECT i1.*
FROM ITEM i1
WHERE i1.ITEM_ID=itemId;
BEGIN
    curDate:= SYSDATE;
    OPEN itemDeals;
    LOOP
        FETCH itemDeals INTO itemDeal;
        EXIT when itemDeals%notfound;
        open itemDetailsArray;
        loop
            FETCH itemDetailsArray into itemDetails;
            EXIT WHEN (itemDetailsArray%NOTFOUND);
            if curDate>=itemDeal.START_DATE and curDate<=itemDeal.END_DATE then  
            itemPrice := itemCount * itemDetails.LISTED_PRICE;
            -- DBMS_OUTPUT.PUT_LINE(itemPrice);

            if itemDeal.percent_deal_type<>1 THEN
                if itemCount>=itemDeal.item_count then
                itemPrice := itemPrice - itemDeal.deal_value*itemPrice/100;
                end if;
            else
            itemPrice := itemPrice - FLOOR(itemCount/itemDeal.item_count)*itemDeal.deal_value;
            end if;
                        -- DBMS_OUTPUT.PUT_LINE(itemPrice);

            itemPrice := itemPrice + itemDetails.tax*itemPrice/100;
            DBMS_OUTPUT.PUT_LINE(itemPrice);

            end if;
        end loop;
        close itemDetailsArray;
    end loop;
        close itemDeals;
END;


-- stored procedure to compute and update total order amount
create or replace PROCEDURE setOrderAmount(orderId IN orders.order_id%TYPE) IS
orderItem ORDER_ITEMS%ROWTYPE;
itemId Item.item_id%type;
itemPrice NUMBER;
orderAmount NUMBER;
CURSOR OrderItems IS
SELECT Oi.* FROM ORDER_ITEMS Oi WHERE Oi.ORDER_ID=orderId ;

BEGIN
    orderAmount:=0;
    OPEN OrderItems;
    LOOP
        FETCH OrderItems INTO orderItem;
        EXIT WHEN (OrderItems%NOTFOUND);
        getItemPrice(orderItem.item_id, orderItem.item_count, itemPrice);
        orderAmount :=orderAmount + itemPrice;
    END LOOP;
    CLOSE OrderItems;
UPDATE ORDERS SET TOTAL_AMOUNT = orderAmount
where ORDER_ID=orderId;
END;



