import React from 'react';
import axios from 'axios';

class StorageListComponent extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            'storages': []
        };

    }

    componentDidMount() {
        this.reload();
    }

    reload(){
        let component = this;

        axios.get('/storage').then(function(result){
            console.log("completed storage ajax request: " + result.data.status + " with data " + result.data.result);
            component.setState({
                'storages': result.data.result
            });
        }).catch(function (error) {
            console.error(error);
        });
    }

    render() {
        return (<table className="dashboardpanel">
            <thead>
            <tr className="dashboardheader">
                <td className="visible">ID</td>
                <td className="visible">Type</td>
                <td className="visible">Root path</td>
                <td className="visible">User</td>
                <td className="visible">Password</td>
                <td className="visible">Host</td>
                <td className="visible">Port</td>
            </tr>
            </thead>
            <tbody>
            {this.state.storages.map(function(storage){
                console.debug(storage);
                return (<tr key={storage.id}>
                    <td className="visible">{storage.id}</td>
                    <td className="visible">{storage.storageType}</td>
                    <td className="visible">{storage.rootpath}</td>
                    <td className="visible">{storage.user}</td>
                    <td className="visible">{storage.password}</td>
                    <td className="visible">{storage.host}</td>
                    <td className="visible">{storage.port}</td>
                </tr>);
            })}
            </tbody>
        </table>);
    }
}

export default StorageListComponent;