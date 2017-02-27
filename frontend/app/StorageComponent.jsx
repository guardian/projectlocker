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
        return (<table>
            <thead>
            <tr>
                <td>ID</td>
                <td>Storage Type</td>
                <td>Root path</td>
                <td>User</td>
                <td>Password</td>
                <td>Host</td>
                <td>Port</td>
            </tr>
            </thead>
            <tbody>
            {this.state.storages.map(function(storage){
                console.debug(storage);
                return (<tr key={storage.id}>
                    <td>{storage.id}</td>
                    <td>{storage.storageType}</td>
                    <td>{storage.rootpath}</td>
                    <td>{storage.user}</td>
                    <td>{storage.password}</td>
                    <td>{storage.host}</td>
                    <td>{storage.port}</td>
                </tr>);
            })}
            </tbody>
        </table>);
    }
}

export default StorageListComponent;