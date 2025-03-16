import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    static class Transaction {
        final int fromId;
        final int toId;
        final int amount;

        Transaction(int fromId, int toId, int amount) {
            this.fromId = fromId;
            this.toId = toId;
            this.amount = amount;
        }
    }

    private static class Bank {
        Map<Integer, Integer> accounts;
        private final Lock lock = new ReentrantLock();
        ExecutorService clerks;

        public Bank(Map<Integer, Integer> accounts) {
            this.accounts = accounts;
            this.clerks = Executors.newCachedThreadPool();
        }

        private void innerProcessTransaction(Transaction transaction) {
            lock.lock();
            try {
                accounts.put(transaction.fromId, accounts.get(transaction.fromId) - transaction.amount);
                accounts.put(transaction.toId, accounts.get(transaction.toId) + transaction.amount);
            } finally {
                lock.unlock();
            }
        }

        public void processTransaction(Transaction transaction) {
            Runnable task = () -> innerProcessTransaction(transaction);
            clerks.execute(task);
        }

        public void closeBank() {
            try {
                clerks.shutdown();
                if (!clerks.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.out.println("Не все транзакции успели завершиться, банковские клерки ушли на отдых.");
                } else {
                    System.out.println("Все транзакции были выполнены успешно.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                clerks.shutdownNow();
            }
        }

        public void getFinalBalance() {
            for (int i = 0; i < accounts.size(); i++) {
                System.out.printf("User %d final balance: %d\n", i, accounts.get(i));
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Bank bank = initBank(scanner);
        int transaction_num = Integer.parseInt(scanner.nextLine());
        Pattern digitRegex = Pattern.compile("\\d+");
        for (int i = 0; i < transaction_num; i++) {
            String transactionDesc = scanner.nextLine();
            Matcher nums = digitRegex.matcher(transactionDesc);
            nums.find();
            int fromId = Integer.parseInt(nums.group());
            nums.find();
            int amount = Integer.parseInt(nums.group());
            nums.find();
            int toId = Integer.parseInt(nums.group());
            Transaction transaction = new Transaction(fromId,toId, amount);
            bank.processTransaction(transaction);
        }
        bank.closeBank();
        bank.getFinalBalance();
     }

    private static Bank initBank(Scanner scanner) {
        int user_num = Integer.parseInt(scanner.nextLine());
        int[] startBalance = Arrays.stream(scanner.nextLine().split(" ")).mapToInt(Integer::parseInt).toArray();
        Map<Integer, Integer> accounts = Collections.synchronizedMap(new HashMap<>());
        for (int i = 0; i < user_num; i++) {
            accounts.put(i, startBalance[i]);
        }
        return new Bank(accounts);
    }

}