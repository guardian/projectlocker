import React from 'react';
import {render} from 'react-dom';
import StorageListComponent from './StorageComponent.jsx';

class App extends React.Component {
    render () {
        return(
            <div>
                <div id="leftmenu" className="leftmenu">
                    <ul>
                        <li><a href="#">Storages...</a></li>
                        <li><a href="#">Project Types...</a></li>
                        <li><a href="#">Project Templates...</a></li>
                        <li><a href="#">Projects...</a></li>
                        <li><a href="#">Files...</a></li>
                    </ul>
                </div>
                <div id="mainbody" className="mainbody">
                    <StorageListComponent/>
                </div>
            </div>
            ) ;
    }
}

render(<App/>, document.getElementById('app'));