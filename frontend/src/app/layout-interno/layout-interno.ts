import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-layout-interno',
  imports: [RouterOutlet],
  templateUrl: './layout-interno.html',
  styleUrls: ['../auth/auth.css', './layout-interno.css'],
})
export class LayoutInterno {}
