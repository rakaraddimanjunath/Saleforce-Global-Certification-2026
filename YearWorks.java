import java.util.Scanner;

public class YearWorks {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter Year: ");
        int year = sc.nextInt();

        System.out.print("Enter first day of year (1-Mon, 2-Tue, 3-Wed, 4-Thu, 5-Fri, 6-Sat, 7-Sun): ");
        int firstDay = sc.nextInt();

        boolean leapYear;

        if ((year % 400 == 0) || (year % 4 == 0 && year % 100 != 0))
            leapYear = true;
        else
            leapYear = false;

        int[] daysInMonth = {
            31,
            leapYear ? 29 : 28,
            31, 30, 31, 30,
            31, 31, 30, 31, 30, 31
        };

        String[] monthNames = {
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        };

        String[] dayNames = {
            "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday", "Saturday", "Sunday"
        };

        int day = firstDay - 1;

        for (int month = 0; month < 12; month++) {

            System.out.println("\n----- " + monthNames[month] + " -----");

            for (int date = 1; date <= daysInMonth[month]; date++) {

                System.out.print(date + " (" + dayNames[day] + ") : ");

                if (day == 2) {
                    System.out.println("Holiday - No Work");
                } else if (day == 0 || day == 3 || day == 5) {
                    System.out.println("Works -> 1 3 5 7");
                } else {
                    System.out.println("Works -> 2 4 6");
                }

                day = (day + 1) % 7;
            }
        }

        sc.close();
    }
}
