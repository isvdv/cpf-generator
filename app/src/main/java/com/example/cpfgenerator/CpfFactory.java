package com.example.cpfgenerator;

import java.security.SecureRandom;

/** Gera CPFs matematicamente válidos para testes; não confirma cadastro real. */
final class CpfFactory {
    private static final SecureRandom RANDOM = new SecureRandom();

    private CpfFactory() {}

    private static int checkDigit(int[] d, int len) {
        int sum = 0;
        for (int i = 0; i < len; i++) sum += d[i] * (len + 1 - i);
        int r = (sum * 10) % 11;
        return r == 10 ? 0 : r;
    }

    static boolean isValid(int[] d) {
        boolean allEqual = true;
        for (int i = 1; i < 11; i++) if (d[i] != d[0]) { allEqual = false; break; }
        if (allEqual) return false;
        return checkDigit(d, 9) == d[9] && checkDigit(d, 10) == d[10];
    }

    /** @param region 9º dígito (0-9) ou -1 para aleatório. Retorna 11 dígitos. */
    static String create(int region) {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 8; i++) d[i] = RANDOM.nextInt(10);
            d[8] = region >= 0 ? region : RANDOM.nextInt(10);
            d[9] = checkDigit(d, 9);
            d[10] = checkDigit(d, 10);
        } while (!isValid(d));
        StringBuilder sb = new StringBuilder(11);
        for (int v : d) sb.append(v);
        return sb.toString();
    }

    static String format(String raw, boolean formatted) {
        if (!formatted) return raw;
        return raw.substring(0, 3) + "." + raw.substring(3, 6) + "." +
                raw.substring(6, 9) + "-" + raw.substring(9, 11);
    }
}
