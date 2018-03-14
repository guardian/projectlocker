import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import StorageSelector from '../../Selectors/StorageSelector.jsx';

class DestinationStorageComponent extends CommonMultistepComponent {
    static propTypes = {
        storageList: PropTypes.array.isRequired,
        selectedStorage: PropTypes.number.isRequired,
        selectionUpdated: PropTypes.func.isRequired
    };

    constructor(props){
        super(props);
    }

    render(){
        return <div>
            <h3>Select destination storage</h3>
            <p>Lastly, we need to know where the project file should be stored</p>
            <table>
                <tbody>
                <tr>
                    <td>Storage</td>
                    <td><StorageSelector selectedStorage={this.props.selectedStorage}
                                         selectionUpdated={this.props.selectionUpdated}
                                         storageList={this.props.storageList}/></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default DestinationStorageComponent;
