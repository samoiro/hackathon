alter table ingredient_price_history
    drop constraint ingredient_price_history_ingredient_id_fkey,
    add constraint ingredient_price_history_ingredient_id_fkey
        foreign key (ingredient_id) references ingredient (id)
        on delete restrict on update restrict;

create index ingredient_price_history_observed_date_idx
    on ingredient_price_history (observed_date);
