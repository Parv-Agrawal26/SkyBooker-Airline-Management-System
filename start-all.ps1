$services = @(
    "skybooker-auth-service",
    "Flight-Service",
    "Seat-Service",
    "Booking-Service",
    "Passenger-Service",
    "Payment-Service",
    "Notification-Service",
    "Airline-Service",
    "SkyBooker-api-gateway"
)

Write-Host "Starting SkyBooker Microservices..." -ForegroundColor Cyan

foreach ($service in $services) {
    Write-Host "Starting $service in a new window..." -ForegroundColor Yellow
    Start-Process -FilePath "cmd.exe" -ArgumentList "/k title $service && cd $service && mvnw.cmd spring-boot:run" -WindowStyle Normal
    
    # Wait a few seconds to avoid port clashes or CPU spikes from simultaneous JVM starts
    Start-Sleep -Seconds 5
}

Write-Host "All services have been started. Check the individual windows for their status." -ForegroundColor Green
