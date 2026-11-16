package io.github.gmailaccountsort;

import java.util.ArrayList;
import java.util.List;

final class AccountOrder {
    interface AddressOf {
        String get(Object account) throws ReflectiveOperationException;
    }

    private AccountOrder() {}

    static List<?> byAddress(List<?> accounts, AddressOf addressOf)
            throws ReflectiveOperationException {
        if (accounts.size() < 2) {
            return accounts;
        }

        List<Integer> positions = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        for (int index = 0; index < accounts.size(); index++) {
            Object account = accounts.get(index);
            String address = account == null ? null : asAddress(addressOf.get(account));
            if (address != null) {
                positions.add(index);
                rows.add(new Row(account, address));
            }
        }

        rows.sort((first, second) -> first.address.compareToIgnoreCase(second.address));
        List<Object> ordered = new ArrayList<>(accounts);
        boolean changed = false;
        for (int index = 0; index < rows.size(); index++) {
            int position = positions.get(index);
            Object account = rows.get(index).account;
            changed |= ordered.set(position, account) != account;
        }
        return changed ? ordered : accounts;
    }

    private static String asAddress(String value) {
        if (value == null) {
            return null;
        }
        String address = value.trim();
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@') || at == address.length() - 1) {
            return null;
        }
        for (int index = 0; index < address.length(); index++) {
            if (Character.isWhitespace(address.charAt(index))) {
                return null;
            }
        }
        return address;
    }

    private static final class Row {
        final Object account;
        final String address;

        Row(Object account, String address) {
            this.account = account;
            this.address = address;
        }
    }
}
