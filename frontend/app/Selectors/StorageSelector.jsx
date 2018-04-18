import React from 'react';
import PropTypes from 'prop-types';

class StorageSelector extends React.Component {
    static propTypes = {
        selectedStorage: PropTypes.number.isRequired,
        selectionUpdated: PropTypes.func.isRequired,
        storageList: PropTypes.array.isRequired,
        enabled: PropTypes.bool.isRequired
    };

    render(){
        return <select id="storageSelector" value={this.props.selectedStorage} disabled={! this.props.enabled}
                onChange={event=>this.props.selectionUpdated(parseInt(event.target.value))}>
            {
                this.props.storageList.map(storage=><option key={storage.id} value={storage.id}>{storage.rootpath} on {storage.storageType}</option>)
            }
        </select>
    }
}

export default StorageSelector;